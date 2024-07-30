package pro.gravit.launchermodules.mirrorhelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class WorkspaceTools {
    private final MirrorHelperModule module;
    private final LaunchServer server;
    private final Logger logger = LogManager.getLogger(WorkspaceTools.class);
    private static final Map<String, BuildInCommand> buildInCommands = new HashMap<>();
    static {
        buildInCommands.put("%download", new DownloadCommand());
        buildInCommands.put("%findJar", new FindJar());
        buildInCommands.put("%fetchManifestValue", new FetchManifestValue());
        buildInCommands.put("%if", new If());
        buildInCommands.put("%updateGradle", new UpdateGradle());
    }

    public WorkspaceTools(MirrorHelperModule module) {
        this.module = module;
        this.server = module.server;
    }

    public void applyWorkspace(MirrorWorkspace workspace, Path workspaceFilePath) throws Exception {
        Path workspacePath = module.getWorkspaceDir();
        Path tmp = Files.createTempDirectory("mirrorhelper");
        try {
            logger.info("Apply workspace");
            logger.info("Download libraries");
            for (var l : workspace.libraries()) {
                if (l.data() != null) {
                    IOHelper.createParentDirs(workspacePath.resolve(l.path()));
                    IOHelper.write(workspacePath.resolve(l.path()), l.data().getBytes(StandardCharsets.UTF_8));
                    continue;
                }
                String randomName = SecurityHelper.randomStringAESKey();
                Path tmpPath = tmp.resolve(randomName);
                logger.info("Download {} to {}", l.url(), tmpPath);
                Downloader.downloadFile(new URI(l.url()), tmpPath, null).getFuture().get();
                if (l.path() != null) {
                    Path lPath = workspacePath.resolve(l.path());
                    IOHelper.createParentDirs(lPath);
                    if (l.prefixFilter() != null) {
                        Predicate<String> pred = (p) -> {
                            for (var e : l.prefixFilter()) {
                                if (p.startsWith(e)) {
                                    return true;
                                }
                            }
                            return false;
                        };
                        try (ZipInputStream input = IOHelper.newZipInput(tmpPath)) {
                            try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(lPath))) {
                                ZipEntry e = input.getNextEntry();
                                while (e != null) {
                                    if (pred.test(e.getName())) {
                                        ZipEntry entry = new ZipEntry(e.getName());
                                        output.putNextEntry(entry);
                                        input.transferTo(output);
                                    }
                                    e = input.getNextEntry();
                                }
                            }
                        }
                    } else {
                        IOHelper.copy(tmpPath, lPath);
                    }
                }
                if (l.unpack() != null) {
                    try (ZipInputStream input = IOHelper.newZipInput(tmpPath)) {
                        ZipEntry e = input.getNextEntry();
                        while (e != null) {
                            String target = l.unpack().get(e.getName());
                            if (target != null) {
                                Path targetPath = workspacePath.resolve(target);
                                IOHelper.createParentDirs(targetPath);
                                try (OutputStream output = IOHelper.newOutput(targetPath)) {
                                    input.transferTo(output);
                                }
                            }
                            e = input.getNextEntry();
                        }
                    }
                }
            }
            logger.info("Download multiMods");
            for (var e : workspace.multiMods().entrySet()) {
                Path target = workspacePath.resolve("multimods").resolve(e.getKey().concat(".jar"));
                logger.info("Download {} to {}", e.getValue().url(), target);
                Downloader.downloadFile(new URI(e.getValue().url()), target, null).getFuture().get();
            }
            logger.info("Install lwjgl3 directory");
            server.commandHandler.findCommand("lwjgldownload").invoke(workspace.lwjgl3version(), "mirrorhelper-tmp-lwjgl3");
            Path lwjgl3Path = workspacePath.resolve("workdir").resolve("lwjgl3");
            IOHelper.move(server.updatesDir.resolve("mirrorhelper-tmp-lwjgl3"), lwjgl3Path);
            Files.deleteIfExists(server.updatesDir.resolve("mirrorhelper-tmp-lwjgl3"));
            logger.info("Save config");
            module.config.workspaceFile = workspaceFilePath.toString();
            module.configurable.saveConfig();
        } finally {
            IOHelper.deleteDir(tmp, true);
        }
    }

    public void build(String scriptName, MirrorWorkspace.BuildScript buildScript, Path clientDir) throws IOException {
        BuildContext context = new BuildContext();
        context.targetClientDir = clientDir;
        context.scriptBuildDir = context.createNewBuildDir(scriptName);
        context.update();
        logger.info("Script build dir {}", context.scriptBuildDir);
        try {
            for(var inst : buildScript.script()) {
                var cmd = inst.cmd().stream().map(context::replace).toList();
                logger.info("Execute {}", String.join(" ", cmd));
                var workdirString = context.replace(inst.workdir());
                Path workdir = workdirString != null ? Path.of(workdirString) : context.scriptBuildDir;
                if(!cmd.isEmpty() && cmd.getFirst().startsWith("%")) {
                    BuildInCommand buildInCommand = buildInCommands.get(cmd.getFirst());
                    if(buildInCommand == null) {
                        throw new IllegalArgumentException(String.format("Build-in command %s not found", cmd.getFirst()));
                    }
                    List<String> cmdArgs = cmd.subList(1, cmd.size());
                    buildInCommand.run(cmdArgs, context, module, server, workdir);
                } else {
                    ProcessBuilder builder = new ProcessBuilder(cmd);
                    builder.inheritIO();
                    builder.directory(workdir.toFile());
                    Process process = builder.start();
                    int code = process.waitFor();
                    if(!inst.ignoreErrorCode() && code != 0) {
                        throw new RuntimeException(String.format("Process exited with code %d", code));
                    }
                }
            }
            if(buildScript.result() != null && buildScript.path() != null) {
                var from = Path.of(context.replace(buildScript.result()));
                var to = buildScript.dynamic() ? clientDir.resolve(context.replace(buildScript.path())) : module.getWorkspaceDir().resolve(buildScript.path());
                logger.info("Copy {} to {}", from, to);
                IOHelper.createParentDirs(to);
                IOHelper.copy(from, to);
            }
            logger.info("Deleting temp dir {}", context.scriptBuildDir);
        } catch (Throwable e){
            logger.error("Build {} failed: {}", scriptName, e);
        }
    }

    private class BuildContext {
        public final Logger logger = LogManager.getLogger(BuildContext.class);
        public Path scriptBuildDir;
        public Path targetClientDir;
        public Map<String, String> variables = new HashMap<>();
        public void update() {
            variables.put("scripttmpdir", scriptBuildDir.toString());
            variables.put("clientdir", targetClientDir.toString());
            variables.put("projectname", server.config.projectName);
        }
        public String replace(String str) {
            if(str == null) {
                return null;
            }
            for(var e : variables.entrySet()) {
                str = str.replace("%"+e.getKey()+"%", e.getValue());
            }
            return str;
        }

        public Path createNewBuildDir(String scriptName) throws IOException {
            return Files.createTempDirectory(scriptName);
        }
    }

    private interface BuildInCommand {
        void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception;
    }

    private static final class DownloadCommand implements BuildInCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception {
            URI uri = new URI(args.get(0));
            Path target = Path.of(args.get(1));
            context.logger.info("Download {} to {}", uri, target);
            Downloader.downloadFile(uri, target, null).getFuture().get();
        }
    }

    private static final class FindJar implements BuildInCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception {
            Path filePath = context.targetClientDir.resolve(args.get(0));
            String varName = args.get(1);
            if(Files.notExists(filePath)) {
                throw new FileNotFoundException(filePath.toAbsolutePath().toString());
            }
            if(Files.isDirectory(filePath)) {
                try(Stream<Path> stream = Files.walk(filePath)) {
                    filePath = stream.filter(e -> !Files.isDirectory(e) && e.getFileName().toString().endsWith(".jar")).findFirst().orElseThrow();
                }
            }
            context.variables.put(varName, filePath.toAbsolutePath().toString());
            if(args.size() >= 3) {
                var version = filePath.getParent().getFileName().toString();
                context.variables.put(args.get(2), version);
            }
        }
    }

    private static final class FetchManifestValue implements BuildInCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception {
            Path filePath = context.targetClientDir.resolve(args.get(0));
            String[] splited = args.get(1).split(",");
            String varName = args.get(2);
            try(JarInputStream input = new JarInputStream(IOHelper.newInput(filePath))) {
                Manifest manifest = input.getManifest();
                Attributes attributes = manifest.getMainAttributes();
                for(var e : splited) {
                    var value = attributes.getValue(e);
                    if(value != null) {
                        context.variables.put(varName, value);
                        return;
                    }
                    for(var entity : manifest.getEntries().entrySet()) {
                        value = entity.getValue().getValue(e);
                        if(value != null) {
                            context.variables.put(varName, value);
                            return;
                        }
                    }
                }
                throw new RuntimeException(String.format("Manifest values %s not found in %s", args.get(1), filePath));
            }
        }
    }

    private static final class UpdateGradle implements BuildInCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception {
            var repoDir = args.get(0);
            var toVersion = args.get(1);
            var propertiesPath = Path.of(repoDir).resolve("gradle").resolve("wrapper").resolve("gradle-wrapper.properties");
            Properties properties = new Properties();
            try(var input = IOHelper.newInput(propertiesPath)) {
                properties.load(input);
            }
            properties.put("distributionUrl", "https://services.gradle.org/distributions/gradle-"+toVersion+"-bin.zip");
            try(var output = IOHelper.newOutput(propertiesPath)) {
                properties.store(output, null);
            }
        }
    }

    private static final class If implements BuildInCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception {
            int ArgOffset = 1;
            boolean ifValue;
            if(args.get(0).equals("version")) {
                var first = ClientProfile.Version.of(args.get(1));
                var op = args.get(2);
                var second = ClientProfile.Version.of(args.get(3));
                ArgOffset += 3;
                ifValue = switch (op) {
                    case ">" -> first.compareTo(second) > 0;
                    case ">=" -> first.compareTo(second) >= 0;
                    case "<" -> first.compareTo(second) < 0;
                    case "<=" -> first.compareTo(second) <= 0;
                    default -> throw new IllegalStateException("Unexpected value: " + op);
                };
            } else {
                throw new UnsupportedOperationException(args.get(0));
            }
            if(ifValue) {
                context.variables.put(args.get(ArgOffset), args.get(ArgOffset+1));
            } else if(args.size() > ArgOffset+1) {
                context.variables.put(args.get(ArgOffset), args.get(ArgOffset+2));
            }
        }
    }
}

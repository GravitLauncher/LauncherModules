package pro.gravit.launchermodules.mirrorhelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.utils.helper.IOHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

final class WorkspaceBuildRunner {
    private final MirrorHelperModule module;
    private final LaunchServer server;
    private final Logger logger = LogManager.getLogger(WorkspaceBuildRunner.class);
    private static final Map<String, BuiltinCommand> builtinCommands = new HashMap<>();
    static {
        builtinCommands.put("%download", new DownloadCommand());
        builtinCommands.put("%findJar", new FindJar());
        builtinCommands.put("%fetchManifestValue", new FetchManifestValue());
        builtinCommands.put("%if", new If());
        builtinCommands.put("%updateGradle", new UpdateGradle());
    }

    WorkspaceBuildRunner(MirrorHelperModule module) {
        this.module = module;
        this.server = module.server;
    }

    public void build(String scriptName, MirrorWorkspace.BuildScript buildScript, Path clientDir) throws Exception {
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
                    BuiltinCommand builtinCommand = builtinCommands.get(cmd.getFirst());
                    if(builtinCommand == null) {
                        throw new IllegalArgumentException(String.format("Built-in command %s not found", cmd.getFirst()));
                    }
                    List<String> cmdArgs = cmd.subList(1, cmd.size());
                    builtinCommand.run(cmdArgs, context, module, server, workdir);
                } else {
                    ProcessBuilder builder = new ProcessBuilder(cmd);
                    builder.inheritIO();
                    builder.directory(workdir.toFile());
                    if (inst.env() != null) {
                        inst.env().forEach((key, value) -> builder.environment().put(key, context.replace(value)));
                    }
                    Process process = builder.start();
                    int code;
                    try {
                        code = process.waitFor();
                    } catch (InterruptedException e) {
                        process.destroyForcibly();
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                    if(!inst.ignoreErrorCode() && code != 0) {
                        throw new RuntimeException(String.format("Process exited with code %d", code));
                    }
                }
            }
            if(buildScript.result() != null && buildScript.path() != null) {
                var from = Path.of(context.replace(buildScript.result()));
                var to = buildScript.dynamic() ? WorkspaceManifest.resolve(clientDir, context.replace(buildScript.path())) : WorkspaceManifest.resolve(module.getWorkspaceDir(), buildScript.path());
                logger.info("Copy {} to {}", from, to);
                IOHelper.createParentDirs(to);
                IOHelper.copy(from, to);
            }
            logger.info("Deleting temp dir {}", context.scriptBuildDir);
        } finally {
            IOHelper.deleteDir(context.scriptBuildDir, true);
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
            return server.createTempDirectory(scriptName);
        }
    }

    private interface BuiltinCommand {
        void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception;
    }

    private static final class DownloadCommand implements BuiltinCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception {
            URI uri = new URI(args.get(0));
            Path target = Path.of(args.get(1));
            context.logger.info("Download {} to {}", uri, target);
            Downloader.downloadFile(uri, target, null).getFuture().get();
        }
    }

    private static final class FindJar implements BuiltinCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception {
            Path filePath = context.targetClientDir.resolve(args.get(0));
            String varName = args.get(1);
            if(Files.notExists(filePath)) {
                throw new FileNotFoundException(filePath.toAbsolutePath().toString());
            }
            if(Files.isDirectory(filePath)) {
                try(Stream<Path> stream = Files.walk(filePath)) {
                    filePath = stream.filter(e -> !Files.isDirectory(e) && e.getFileName().toString().endsWith(".jar")).sorted().findFirst().orElseThrow();
                }
            }
            context.variables.put(varName, filePath.toAbsolutePath().toString());
            if(args.size() >= 3) {
                var version = filePath.getParent().getFileName().toString();
                context.variables.put(args.get(2), version);
            }
        }
    }

    private static final class FetchManifestValue implements BuiltinCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) throws Exception {
            Path filePath = context.targetClientDir.resolve(args.get(0));
            String[] splited = args.get(1).split(",");
            String varName = args.get(2);
            try(JarInputStream input = new JarInputStream(IOHelper.newInput(filePath))) {
                Manifest manifest = input.getManifest();
                if (manifest == null) {
                    throw new IOException("Missing JAR manifest: " + filePath);
                }
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

    private static final class UpdateGradle implements BuiltinCommand {

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

    private static final class If implements BuiltinCommand {

        @Override
        public void run(List<String> args, BuildContext context, MirrorHelperModule module, LaunchServer server, Path workdir) {
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
                throw new UnsupportedOperationException(args.getFirst());
            }
            if(ifValue) {
                context.variables.put(args.get(ArgOffset), args.get(ArgOffset+1));
            } else if(args.size() > ArgOffset+2) {
                context.variables.put(args.get(ArgOffset), args.get(ArgOffset+2));
            }
        }
    }
}

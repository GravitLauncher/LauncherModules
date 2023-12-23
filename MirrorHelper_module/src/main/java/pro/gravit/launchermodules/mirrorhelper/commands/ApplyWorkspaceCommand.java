package pro.gravit.launchermodules.mirrorhelper.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.modern.Downloader;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.MirrorWorkspace;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ApplyWorkspaceCommand extends Command {
    private final MirrorHelperModule module;
    private final Logger logger = LogManager.getLogger(ApplyWorkspaceCommand.class);
    private static final Map<String, BuildInCommand> buildInCommands = new HashMap<>();
    static {
        buildInCommands.put("%download", new DownloadCommand());
    }

    public ApplyWorkspaceCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[path]";
    }

    @Override
    public String getUsageDescription() {
        return "apply workspace. This action remove your files in workspace!";
    }

    @Override
    public void invoke(String... args) throws Exception {
        URI url = null;
        Path workspaceFilePath = null;
        if(args.length == 0) {
            url = server.mirrorManager.getDefaultMirror().getURL("workspace.json").toURI();
        } else if(args[0].startsWith("http://") || args[0].startsWith("https://")) {
            url = new URI(args[0]);
        } else {
            workspaceFilePath = Paths.get(args[0]);
        }
        if(url != null) {
            workspaceFilePath = module.getConfigDir().resolve("workspace.json");
            logger.info("Download {} to {}", url, workspaceFilePath);
            Downloader.downloadFile(url, workspaceFilePath, null).getFuture().get();
        }
        MirrorWorkspace workspace;
        try(Reader reader = IOHelper.newReader(workspaceFilePath)) {
            workspace = Launcher.gsonManager.gson.fromJson(reader, MirrorWorkspace.class);
        }
        Path workspacePath = module.getWorkspaceDir();
        if(Files.exists(workspacePath)) {
            logger.warn("THIS ACTION DELETE ALL FILES IN {}", workspacePath);
            if(!showApplyDialog("Continue?")) {
                return;
            }
            IOHelper.deleteDir(workspacePath, false);
        } else {
            Files.createDirectories(workspacePath);
        }
        Path tmp = Files.createTempDirectory("mirrorhelper");
        logger.info("Download libraries");
        try {
            for(var l : workspace.libraries()) {
                if(l.data() != null) {
                    IOHelper.createParentDirs(workspacePath.resolve(l.path()));
                    IOHelper.write(workspacePath.resolve(l.path()), l.data().getBytes(StandardCharsets.UTF_8));
                    continue;
                }
                String randomName = SecurityHelper.randomStringAESKey();
                Path tmpPath = tmp.resolve(randomName);
                logger.info("Download {} to {}", l.url(), tmpPath);
                Downloader.downloadFile(new URI(l.url()), tmpPath, null).getFuture().get();
                if(l.path() != null) {
                    Path lPath = workspacePath.resolve(l.path());
                    IOHelper.createParentDirs(lPath);
                    if(l.prefixFilter() != null) {
                        Predicate<String> pred = (p) -> {
                            for(var e : l.prefixFilter()) {
                                if(p.startsWith(e)) {
                                    return true;
                                }
                            }
                            return false;
                        };
                        try(ZipInputStream input = IOHelper.newZipInput(tmpPath)) {
                            try(ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(lPath))) {
                                ZipEntry e = input.getNextEntry();
                                while(e != null) {
                                    if(pred.test(e.getName())) {
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
                if(l.unpack() != null) {
                    try(ZipInputStream input = IOHelper.newZipInput(tmpPath)) {
                        ZipEntry e = input.getNextEntry();
                        while(e != null) {
                            String target = l.unpack().get(e.getName());
                            if(target != null) {
                                Path targetPath = workspacePath.resolve(target);
                                IOHelper.createParentDirs(targetPath);
                                try(OutputStream output = IOHelper.newOutput(targetPath)) {
                                    input.transferTo(output);
                                }
                            }
                            e = input.getNextEntry();
                        }
                    }
                }
            }
            logger.info("Build from sources");
            BuildContext context = new BuildContext();
            for(var e : workspace.build().entrySet()) {
                build(context, e.getKey(), e.getValue());
            }
            logger.info("Download multiMods");
            for(var e : workspace.multiMods().entrySet()) {
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
            logger.info("Complete");
        } finally {
            IOHelper.deleteDir(tmp, true);
        }
    }

    private void build(BuildContext context, String scriptName, MirrorWorkspace.BuildScript buildScript) throws IOException {
        context.scriptBuildDir = context.createNewBuildDir(scriptName);
        logger.info("Script build dir {}", context.scriptBuildDir);
        try {
            for(var inst : buildScript.script()) {
                var cmd = inst.cmd().stream().map(context::replace).toList();
                logger.info("Execute {}", String.join(" ", cmd));
                var workdirString = context.replace(inst.workdir());
                Path workdir = workdirString != null ? Path.of(workdirString) : context.scriptBuildDir;
                if(!cmd.isEmpty() && cmd.get(0).startsWith("%")) {
                    BuildInCommand buildInCommand = buildInCommands.get(cmd.get(0));
                    if(buildInCommand == null) {
                        throw new IllegalArgumentException(String.format("Build-in command %s not found", cmd.get(0)));
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
                var to = module.getWorkspaceDir().resolve(buildScript.path());
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
        public String replace(String str) {
            if(str == null) {
                return null;
            }
            return str
                    .replace("%scripttmpdir%", scriptBuildDir.toString())
                    .replace("%projectname%", server.config.projectName);
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
}

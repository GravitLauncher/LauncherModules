package pro.gravit.launchermodules.mirrorhelper.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.MirrorWorkspace;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.HttpDownloader;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ApplyWorkspaceCommand extends Command {
    private final MirrorHelperModule module;
    private final Logger logger = LogManager.getLogger(ApplyWorkspaceCommand.class);

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
        verifyArgs(args, 1);
        Path workspaceFilePath = Paths.get(args[0]);
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
                HttpDownloader.downloadFile(new URL(l.url()), tmpPath, (tr) -> {});
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
            logger.info("Download multiMods");
            for(var e : workspace.multiMods().entrySet()) {
                Path target = workspacePath.resolve("multimods").resolve(e.getKey().concat(".jar"));
                logger.info("Download {} to {}", e.getValue().url(), target);
                HttpDownloader.downloadFile(new URL(e.getValue().url()), target, (tr) -> {});
            }
            logger.info("Install lwjgl3 directory");
            server.commandHandler.findCommand("lwjgldownload").invoke(workspace.lwjgl3version(), "mirrorhelper-tmp-lwjgl3");
            Path lwjgl3Path = workspacePath.resolve("workdir").resolve("lwjgl3");
            IOHelper.move(server.updatesDir.resolve("mirrorhelper-tmp-lwjgl3"), lwjgl3Path);
            Files.deleteIfExists(server.updatesDir.resolve("mirrorhelper-tmp-lwjgl3"));
            logger.info("Save config");
            module.configurable.saveConfig();
            logger.info("Complete");
        } finally {
            IOHelper.deleteDir(tmp, true);
        }
    }
}

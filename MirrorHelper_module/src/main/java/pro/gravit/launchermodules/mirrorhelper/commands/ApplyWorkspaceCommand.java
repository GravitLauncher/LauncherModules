package pro.gravit.launchermodules.mirrorhelper.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.Downloader;
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
        module.tools.applyWorkspace(workspace, workspaceFilePath);
        logger.info("Complete");
    }
}

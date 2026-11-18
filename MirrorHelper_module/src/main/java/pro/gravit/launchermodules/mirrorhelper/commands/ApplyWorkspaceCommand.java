package pro.gravit.launchermodules.mirrorhelper.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.WorkspaceManifest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        return "Validate and activate a workspace manifest";
    }

    @Override
    public void invoke(String... args) throws Exception {
        if (args.length > 1) {
            throw new IllegalArgumentException("Expected at most one workspace path or URL");
        }
        if (args.length == 1 && !args[0].startsWith("http://") && !args[0].startsWith("https://")) {
            module.applyWorkspace(Paths.get(args[0]), true);
        } else {
            URI uri = WorkspaceManifest.remoteUri(args.length == 0
                    ? server.mirrorManager.getDefaultMirror().getURL("workspace.json").toString()
                    : args[0]);
            Path temporary = Files.createTempFile("mirrorhelper-manifest-", ".json");
            try {
                Downloader.downloadFile(uri, temporary, null).getFuture().get();
                module.applyWorkspace(temporary, false);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        logger.info("Workspace activated");
    }
}

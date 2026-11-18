package pro.gravit.launchermodules.mirrorhelper.commands;

import pro.gravit.launchermodules.mirrorhelper.ModInstaller;
import pro.gravit.launchermodules.mirrorhelper.WorkspaceManifest;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class InstallModCommand extends Command {

    private final MirrorHelperModule module;

    public InstallModCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[dir] [version] [forge/fabric] [mod1,mod2,mod3]";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 4);
        Path dir = WorkspaceManifest.resolve(server.updatesDir, WorkspaceManifest.component(args[0]));
        if (!Files.isDirectory(dir)) {
            throw new FileNotFoundException(dir.toString());
        }
        synchronized (module) {
            ModInstaller.install(module.config.curseforgeApiKey, WorkspaceManifest.resolve(dir, "mods"),
                    args[2], parseClientVersion(args[1]), Arrays.asList(args[3].split(",")));
        }
    }
}

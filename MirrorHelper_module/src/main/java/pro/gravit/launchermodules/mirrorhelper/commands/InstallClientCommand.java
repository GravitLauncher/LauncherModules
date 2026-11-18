package pro.gravit.launchermodules.mirrorhelper.commands;

import pro.gravit.launchermodules.mirrorhelper.InstallClient;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.MirrorWorkspace;
import pro.gravit.launchserver.LaunchServer;

import java.util.List;

public class InstallClientCommand extends AbstractInstallClientCommand {
    private final MirrorHelperModule module;

    public InstallClientCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[name] [version] [versionType] (mods)";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    public void invoke(String... args) throws Exception {
        var target = parseTarget(args, 3);
        synchronized (module) {
            MirrorWorkspace workspace = module.getWorkspace();
            List<String> mods = mods(args, 3, workspace, target.loader());
            new InstallClient(module, target.name(), target.version(), mods, target.loader(), workspace).run();
        }
    }

}

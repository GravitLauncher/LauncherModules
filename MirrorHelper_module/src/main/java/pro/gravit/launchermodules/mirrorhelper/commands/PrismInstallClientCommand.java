package pro.gravit.launchermodules.mirrorhelper.commands;

import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.MirrorWorkspace;
import pro.gravit.launchermodules.mirrorhelper.PrismInstallClient;
import pro.gravit.launchserver.LaunchServer;

import java.util.List;

public class PrismInstallClientCommand extends AbstractInstallClientCommand {
    private final MirrorHelperModule module;

    public PrismInstallClientCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[name] [version] [versionType] [prismClientName] (mods)";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    public void invoke(String... args) throws Exception {
        var target = parseTarget(args, 4);
        synchronized (module) {
            MirrorWorkspace workspace = module.getWorkspace();
            List<String> mods = mods(args, 4, workspace, target.loader());
            new PrismInstallClient(module, target.name(), target.version(), mods, target.loader(), workspace, args[3]).run();
        }
    }

}

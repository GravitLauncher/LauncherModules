package pro.gravit.launchermodules.launchermoduleloader;

import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class SyncLauncherModulesCommand extends Command {
    public final transient LauncherModuleLoaderModule module;

    public SyncLauncherModulesCommand(LauncherModuleLoaderModule module) {
        this.module = module;
    }


    @Override
    public String getArgsDescription() {
        return "Resync launcher modules";
    }

    @Override
    public String getUsageDescription() {
        return "[]";
    }

    @Override
    public void invoke(String... args) throws Exception {
        module.syncModules();
        LogHelper.info("Launcher Modules synced");
    }
}

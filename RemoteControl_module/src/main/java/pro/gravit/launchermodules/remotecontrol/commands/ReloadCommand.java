package pro.gravit.launchermodules.remotecontrol.commands;

import pro.gravit.launchermodules.remotecontrol.RemoteControlModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class ReloadCommand extends Command {
    public ReloadCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "Reload config";
    }

    @Override
    public void invoke(String... args) throws Exception {
        RemoteControlModule module = server.modulesManager.getModule(RemoteControlModule.class);
        module.configurable.loadConfig();
        LogHelper.info("RemoteControl config reloaded");
    }
}

package pro.gravit.launchermodules.remotecontrol.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launchermodules.remotecontrol.RemoteControlModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class ReloadCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(ReloadCommand.class);

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
        logger.info("RemoteControl config reloaded");
    }
}
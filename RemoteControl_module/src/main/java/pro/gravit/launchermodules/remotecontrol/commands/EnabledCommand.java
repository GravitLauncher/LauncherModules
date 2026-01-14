package pro.gravit.launchermodules.remotecontrol.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launchermodules.remotecontrol.RemoteControlModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class EnabledCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(EnabledCommand.class);

    public EnabledCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[true/false]";
    }

    @Override
    public String getUsageDescription() {
        return "enable or disable RemoteControl module";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        RemoteControlModule module = server.modulesManager.getModule(RemoteControlModule.class);
        module.config.enabled = Boolean.parseBoolean(args[0]);
        module.configurable.saveConfig();
        logger.info("Param config.enabled updated");
    }
}
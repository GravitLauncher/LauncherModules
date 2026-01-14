package pro.gravit.launchermodules.remotecontrol.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launchermodules.remotecontrol.RemoteControlConfig;
import pro.gravit.launchermodules.remotecontrol.RemoteControlModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class ListCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(ListCommand.class);

    public ListCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    public void invoke(String... args) {
        RemoteControlModule module = server.modulesManager.getModule(RemoteControlModule.class);
        for (RemoteControlConfig.RemoteControlToken token : module.config.list) {
            logger.info("Token {}... allow {} commands {}", token.token.substring(0, 5), token.allowAll ? "all" : String.valueOf(token.commands.size()), token.commands.isEmpty() ? "" : String.join(", ", token.commands));
        }
        logger.info("Found {} tokens", module.config.list.size());
    }
}
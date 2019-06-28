package pro.gravit.launchermodules.sashoksupport.command;

import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.LogHelper;

public final class LogConnectionsCommand extends Command {
    public LogConnectionsCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[componentName] [true/false]";
    }

    @Override
    public String getUsageDescription() {
        return "Enable or disable logging connections";
    }

    @Override
    public void invoke(String... args) throws CommandException {
        verifyArgs(args, 2);
        LegacyServerComponent component = (LegacyServerComponent) server.config.components.get(args[0]);
        boolean newValue;
        if (args.length >= 2) {
            newValue = Boolean.parseBoolean(args[0]);
            component.handler.logConnections = newValue;
        } else
            newValue = component.handler.logConnections;
        LogHelper.subInfo("Log connections: " + newValue);
    }
}

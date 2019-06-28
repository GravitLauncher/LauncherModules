package pro.gravit.launchermodules.sashoksupport.command;

import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.CommonHelper;

public final class RebindCommand extends Command {
    public RebindCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[componentName]";
    }

    @Override
    public String getUsageDescription() {
        return "Rebind server socket";
    }

    @Override
    public void invoke(String... args) throws CommandException {
        verifyArgs(args, 1);
        LegacyServerComponent component = (LegacyServerComponent) server.config.components.get(args[0]);
        component.handler.close();
        CommonHelper.newThread("Legacy Sashok Server", true, component.handler);
    }
}

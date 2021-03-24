package pro.gravit.launchermodules.remotecontrol.commands;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class RemoteControlCommand extends Command {
    public RemoteControlCommand(LaunchServer server) {
        super(server);
        childCommands.put("reload", new ReloadCommand(server));
        childCommands.put("revoke", new RevokeCommand(server));
        childCommands.put("new", new NewCommand(server));
        childCommands.put("enabled", new EnabledCommand(server));
        childCommands.put("list", new ListCommand(server));
    }

    @Override
    public String getArgsDescription() {
        return "[subcommand] [args]";
    }

    @Override
    public String getUsageDescription() {
        return "Manage RemoteControl module";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}

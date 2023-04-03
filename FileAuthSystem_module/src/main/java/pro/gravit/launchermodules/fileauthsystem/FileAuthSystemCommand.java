package pro.gravit.launchermodules.fileauthsystem;

import pro.gravit.launchermodules.fileauthsystem.commands.InstallCommand;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class FileAuthSystemCommand extends Command {

    public FileAuthSystemCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.childCommands.put("install", new InstallCommand(server, module));
    }

    @Override
    public String getArgsDescription() {
        return "[subcommand]";
    }

    @Override
    public String getUsageDescription() {
        return "manage FileAuthSystem";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}

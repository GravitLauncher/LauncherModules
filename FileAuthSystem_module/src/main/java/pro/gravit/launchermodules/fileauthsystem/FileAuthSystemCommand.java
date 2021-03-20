package pro.gravit.launchermodules.fileauthsystem;

import pro.gravit.launchermodules.fileauthsystem.commands.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class FileAuthSystemCommand extends Command {
    private final FileAuthSystemModule module;

    public FileAuthSystemCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.module = module;
        this.childCommands.put("register", new RegisterCommand(server, module));
        this.childCommands.put("getuser", new GetUserCommand(server, module));
        this.childCommands.put("getusers", new GetUsersCommand(server, module));
        this.childCommands.put("changepassword", new ChangePasswordCommand(server, module));
        this.childCommands.put("givepermission", new GivePermissionCommand(server, module));
        this.childCommands.put("giveflag", new GiveFlagCommand(server, module));
        this.childCommands.put("reload", new ReloadCommand(server, module));
        this.childCommands.put("save", new SaveCommand(server, module));
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

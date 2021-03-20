package pro.gravit.launchermodules.fileauthsystem.commands;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class RegisterCommand extends Command {
    private final FileAuthSystemModule module;

    public RegisterCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[username] [password]";
    }

    @Override
    public String getUsageDescription() {
        return "register user";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        FileAuthSystemModule.UserEntity entity = new FileAuthSystemModule.UserEntity(args[0], args[1]);
        module.addUser(entity);
    }
}

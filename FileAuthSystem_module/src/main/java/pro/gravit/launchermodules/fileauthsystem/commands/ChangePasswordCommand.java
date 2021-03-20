package pro.gravit.launchermodules.fileauthsystem.commands;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class ChangePasswordCommand extends Command {
    private final FileAuthSystemModule module;

    public ChangePasswordCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[username] [password]";
    }

    @Override
    public String getUsageDescription() {
        return "change user password";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        FileAuthSystemModule.UserEntity entity = module.getUser(args[0]);
        if (entity == null)
            throw new IllegalArgumentException(String.format("User %s not found", args[0]));
        entity.setPassword(args[1]);
        LogHelper.info("Password changed");
    }
}

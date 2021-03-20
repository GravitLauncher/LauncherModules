package pro.gravit.launchermodules.fileauthsystem.commands;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class GetUserCommand extends Command {
    private final FileAuthSystemModule module;

    public GetUserCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[username]";
    }

    @Override
    public String getUsageDescription() {
        return "print user data";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        FileAuthSystemModule.UserEntity entity = module.getUser(args[0]);
        if (entity == null)
            throw new IllegalArgumentException(String.format("User %s not found", args[0]));
        LogHelper.info("[%s] UUID: %s | permissions %d | flags %d", entity.username, entity.uuid, entity.permissions.permissions, entity.permissions.flags);
    }
}

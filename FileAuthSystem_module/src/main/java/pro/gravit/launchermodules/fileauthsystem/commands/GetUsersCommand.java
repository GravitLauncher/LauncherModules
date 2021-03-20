package pro.gravit.launchermodules.fileauthsystem.commands;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class GetUsersCommand extends Command {
    private final FileAuthSystemModule module;

    public GetUsersCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "";
    }

    @Override
    public String getUsageDescription() {
        return "print all users data";
    }

    @Override
    public void invoke(String... args) throws Exception {
        int num = 0;
        for (FileAuthSystemModule.UserEntity entity : module.getAllUsers()) {
            LogHelper.info("[%s] UUID: %s | permissions %d | flags %d", entity.username, entity.uuid, entity.permissions.permissions, entity.permissions.flags);
            num++;
        }
        LogHelper.info("Found %d users", num);
    }
}

package pro.gravit.launchermodules.fileauthsystem.commands;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class GiveFlagCommand extends Command {
    private final FileAuthSystemModule module;

    public GiveFlagCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[username] [flag] [true/false]";
    }

    @Override
    public String getUsageDescription() {
        return "set flag for user";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        FileAuthSystemModule.UserEntity user = module.getUser(args[0]);
        if (user == null) {
            LogHelper.error("User %s not found", args[0]);
            return;
        }
        ClientPermissions permissions = user.permissions;
        long perm = Long.parseLong(args[1]);
        boolean value = Boolean.parseBoolean(args[2]);
        permissions.setFlag(perm, value);
        LogHelper.info("User updated");
    }
}

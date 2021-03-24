package pro.gravit.launchermodules.remotecontrol.commands;

import pro.gravit.launchermodules.remotecontrol.RemoteControlConfig;
import pro.gravit.launchermodules.remotecontrol.RemoteControlModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class RevokeCommand extends Command {
    public RevokeCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[token]";
    }

    @Override
    public String getUsageDescription() {
        return "Revoke token";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        String token = args[0];
        RemoteControlModule module = server.modulesManager.getModule(RemoteControlModule.class);
        RemoteControlConfig.RemoteControlToken token1 = null;
        for (RemoteControlConfig.RemoteControlToken t : module.config.list) {
            if (t.token.startsWith(token)) {
                token1 = t;
                break;
            }
        }
        if (token1 == null) {
            throw new IllegalArgumentException("Token not found");
        }
        module.config.list.remove(token1);
        LogHelper.info("Token %s removed", token1.token);
    }
}

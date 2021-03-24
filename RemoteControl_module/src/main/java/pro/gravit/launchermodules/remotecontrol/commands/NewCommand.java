package pro.gravit.launchermodules.remotecontrol.commands;

import pro.gravit.launchermodules.remotecontrol.RemoteControlConfig;
import pro.gravit.launchermodules.remotecontrol.RemoteControlModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.Arrays;

public class NewCommand extends Command {
    public NewCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[allowAll] [startWithMode] (commands...)";
    }

    @Override
    public String getUsageDescription() {
        return "add new token";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        RemoteControlModule module = server.modulesManager.getModule(RemoteControlModule.class);
        String token = SecurityHelper.randomStringToken();
        RemoteControlConfig.RemoteControlToken token1 = new RemoteControlConfig.RemoteControlToken();
        token1.allowAll = Boolean.parseBoolean(args[0]);
        token1.startWithMode = Boolean.parseBoolean(args[1]);
        token1.token = token;
        if (args.length > 2) {
            token1.commands.addAll(Arrays.asList(args).subList(2, args.length));
        }
        module.config.list.add(token1);
        LogHelper.info("Add new token: %s with %s commands", token, token1.allowAll ? "all" : String.valueOf(token1.commands.size()));
        module.configurable.saveConfig();
        LogHelper.info("RemoteControl module config saved");
    }
}

package pro.gravit.launchermodules.unsafecommands;

import pro.gravit.launchermodules.unsafecommands.impl.AllowSecurityManager;
import pro.gravit.launchermodules.unsafecommands.impl.LoggerSecurityManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class SetSystemSecurityManagerCommand extends Command {
    protected SetSystemSecurityManagerCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[allow, logger, system]";
    }

    @Override
    public String getUsageDescription() {
        return "Set system SecurityManager";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        if(args[0].equals("allow"))
        {
            System.setSecurityManager(new AllowSecurityManager());
        }
        else if(args[0].equals("logger"))
        {
            System.setSecurityManager(new LoggerSecurityManager());
        }
        else if(args[0].equals("system"))
        {
            System.setSecurityManager(new SecurityManager());
        }
    }
}

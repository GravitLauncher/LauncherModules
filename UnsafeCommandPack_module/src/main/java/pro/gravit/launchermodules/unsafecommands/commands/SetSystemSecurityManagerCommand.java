package pro.gravit.launchermodules.unsafecommands.commands;

import pro.gravit.launchermodules.unsafecommands.impl.AllowSecurityManager;
import pro.gravit.launchermodules.unsafecommands.impl.LoggerSecurityManager;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class SetSystemSecurityManagerCommand extends Command {
    public SetSystemSecurityManagerCommand(LaunchServer server) {
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
        switch (args[0]) {
            case "allow":
                System.setSecurityManager(new AllowSecurityManager());
                break;
            case "logger":
                System.setSecurityManager(new LoggerSecurityManager());
                break;
            case "system":
                System.setSecurityManager(new SecurityManager());
                break;
        }
    }
}

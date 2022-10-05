package pro.gravit.launchermodules.swiftupdates;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class SwiftUpdatesCleanupCommand extends Command {
    private final SwiftService swiftService;
    private final SwiftService.Config config;

    protected SwiftUpdatesCleanupCommand(LaunchServer server, SwiftService swiftService, SwiftService.Config config) {
        super(server);
        this.swiftService = swiftService;
        this.config = config;
    }

    @Override
    public String getArgsDescription() {
        return "([container] [prefix])";
    }

    @Override
    public String getUsageDescription() {
        return "clean objects that are present on remote storage with said prefix (config values as default)";
    }

    @Override
    public void invoke(String... args) throws Exception {
        if (args.length == 0) {
            swiftService.cleanupContainer(config.openStackContainer, config.behavior.prefix);
        } else {
            swiftService.cleanupContainer(args[0], args[1]);
        }
    }
}

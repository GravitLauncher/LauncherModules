package pro.gravit.launchermodules.swiftupdates;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class SwiftUpdatesUploadCommand extends Command {
    private final SwiftService swiftService;
    private final SwiftService.Config config;

    protected SwiftUpdatesUploadCommand(LaunchServer server, SwiftService swiftService, SwiftService.Config config) {
        super(server);
        this.swiftService = swiftService;
        this.config = config;
    }

    @Override
    public String getArgsDescription() {
        return "([container] [prefix] [forceupdload])";
    }

    @Override
    public String getUsageDescription() {
        return "upload updates files to the container with specified prefix (config values as defaults)";
    }

    @Override
    public void invoke(String... args) throws Exception {
        if (args.length == 0) {
            swiftService.uploadDir(server.updatesDir, config.openStackContainer, config.behavior.prefix, config.behavior.forceUpload);
        } else {
            swiftService.uploadDir(server.updatesDir, args[0], args[1], Boolean.parseBoolean(args[2]));
        }
    }
}

package pro.gravit.launchermodules.s3updates;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class S3UpdatesCleanupCommand extends Command {
    private final S3Service s3Service;
    private final S3Service.Config config;

    protected S3UpdatesCleanupCommand(LaunchServer server, S3Service s3Service, S3Service.Config config) {
        super(server);
        this.s3Service = s3Service;
        this.config = config;
    }

    @Override
    public String getArgsDescription() {
        return "([container] [prefix])";
    }

    @Override
    public String getUsageDescription() {
        return "cleans objects that are present on S3 storage with said prefix (config values as default)";
    }

    @Override
    public void invoke(String... args) {
        if (args.length == 0) {
            s3Service.cleanupBucket(config.s3Bucket, config.behavior.prefix);
        } else {
            s3Service.cleanupBucket(args[0], args[1]);
        }
    }
}

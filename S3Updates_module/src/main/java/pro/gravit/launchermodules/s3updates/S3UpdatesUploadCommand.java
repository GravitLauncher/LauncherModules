package pro.gravit.launchermodules.s3updates;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class S3UpdatesUploadCommand extends Command {
    private final S3Service s3Service;
    private final S3Service.Config config;

    protected S3UpdatesUploadCommand(LaunchServer server, S3Service s3Service, S3Service.Config config) {
        super(server);
        this.s3Service = s3Service;
        this.config = config;
    }

    @Override
    public String getArgsDescription() {
        return "([container] [prefix] [forceupdload])";
    }

    @Override
    public String getUsageDescription() {
        return "upload update files to the S3 storage with specified prefix (config values as defaults)";
    }

    @Override
    public void invoke(String... args) {
        if (args.length == 0) {
            s3Service.uploadDir(server.updatesDir, config.s3Bucket, config.behavior.prefix, config.behavior.forceUpload, server.updatesManager);
        } else {
            s3Service.uploadDir(server.updatesDir, args[0], args[1], Boolean.parseBoolean(args[2]), server.updatesManager);
        }
    }
}

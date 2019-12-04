package pro.gravit.launchermodules.unsafecommands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import pro.gravit.launchermodules.unsafecommands.impl.AssetDownloader;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class NewDownloadAssetCommand extends Command {
    protected NewDownloadAssetCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[version] [dir]";
    }

    @Override
    public String getUsageDescription() {
        return "Download asset dir";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        String version = args[0];
        String dirName = IOHelper.verifyFileName(args[1]);
        Path assetDir = server.updatesDir.resolve(dirName);

        // Create asset dir
        LogHelper.subInfo("Creating asset dir: '%s'", dirName);
        Files.createDirectory(assetDir);

        LogHelper.subInfo("Getting asset index, it may take some time");
        List<DownloadTask> applies = AssetDownloader.listAssets(assetDir, version);
        // Download required asset
        LogHelper.subInfo("Downloading asset, it may take some time");
        //TODO: Replace
        //new ListDownloader().download(AssetDownloader.getBase(), applies, assetDir, (f, d, t) -> { }, (e) -> { });
        LogHelper.subInfo("Asset successfully downloaded: '%s'", dirName);
        // Finished
        server.syncUpdatesDir(Collections.singleton(dirName));
    }
}
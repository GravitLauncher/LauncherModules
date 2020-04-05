package pro.gravit.launchermodules.unsafecommands;

import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.launchermodules.unsafecommands.impl.AssetDownloader;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        List<AsyncDownloader.SizedFile> applies = AssetDownloader.listAssets(assetDir, version);
        // Download required asset
        LogHelper.subInfo("Downloading asset, it may take some time");
        AsyncDownloader d = new AsyncDownloader();
        ExecutorService e = Executors.newFixedThreadPool(4);
        CompletableFuture.allOf(d.runDownloadList(d.sortFiles(applies, 4), AssetDownloader.getBase(), assetDir, e)).thenAccept((v) -> {
            LogHelper.subInfo("Asset successfully downloaded: '%s'", dirName);
        }).get();
        e.awaitTermination(4, TimeUnit.HOURS);
        e.shutdown();
        e.awaitTermination(4, TimeUnit.HOURS);
        // Finished
        server.syncUpdatesDir(Collections.singleton(dirName));
    }
}
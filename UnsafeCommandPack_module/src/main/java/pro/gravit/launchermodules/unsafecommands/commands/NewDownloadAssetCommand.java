package pro.gravit.launchermodules.unsafecommands.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.launchermodules.unsafecommands.impl.AssetDownloader;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.Downloader;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewDownloadAssetCommand extends Command {
    private final Logger logger = LogManager.getLogger();
    public NewDownloadAssetCommand(LaunchServer server) {
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
        ExecutorService e = Executors.newFixedThreadPool(4);
        long total = 0;
        for (AsyncDownloader.SizedFile file : applies) {
            total += file.size;
        }
        final long finalTotal = total;
        logger.info("Download started, total {}", finalTotal);
        Downloader downloader = Downloader.downloadList(applies, AssetDownloader.getBase(), assetDir, new Downloader.DownloadCallback() {
            private long lastTime = System.currentTimeMillis();
            private long current = 0;
            private long currentFiles = 0;

            @Override
            public void apply(long fullDiff) {
                current += fullDiff;
                if (System.currentTimeMillis() - lastTime > 1000) {
                    lastTime = System.currentTimeMillis();
                    logger.debug("Download progress {} / {} | {} / {}", current, finalTotal, currentFiles, applies.size());
                }
            }

            @Override
            public void onComplete(Path path) {
                currentFiles++;
            }
        }, e, 4);
        downloader.getFuture().get();
        e.shutdownNow();
        // Finished
        server.syncUpdatesDir(Collections.singleton(dirName));
    }
}
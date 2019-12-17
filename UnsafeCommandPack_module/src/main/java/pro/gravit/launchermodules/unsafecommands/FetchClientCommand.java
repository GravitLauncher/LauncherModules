package pro.gravit.launchermodules.unsafecommands;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.launchermodules.unsafecommands.impl.AssetDownloader;
import pro.gravit.launchermodules.unsafecommands.impl.ClientDownloader;
import pro.gravit.launchermodules.unsafecommands.impl.ClientDownloader.Artifact;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class FetchClientCommand extends Command {
    protected FetchClientCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[version] [dir]";
    }

    @Override
    public String getUsageDescription() {
        return "Download client dir (without profile)";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        String version = args[0];
        String dirName = IOHelper.verifyFileName(args[1]);
        Path clientDir = server.updatesDir.resolve(dirName);

        // Create asset dir
        LogHelper.subInfo("Creating client dir: '%s'", dirName);
        Files.createDirectory(clientDir);

        LogHelper.subInfo("Getting client info, it may take some time");
        ClientDownloader.ClientInfo info = ClientDownloader.getClient(version);
        // Download required files
        LogHelper.subInfo("Downloading client, it may take some time");
        AsyncDownloader d = new AsyncDownloader();
        ExecutorService e = Executors.newFixedThreadPool(4);
        List<AsyncDownloader.SizedFile> applies = info.libraries.stream().map(y -> new AsyncDownloader.SizedFile(y.url.replace("https://libraries\\.minecraft\\.net/", ""), y.path, y.size)).collect(Collectors.toList());
        CompletableFuture.allOf(d.runDownloadList(d.sortFiles(applies, 4), AssetDownloader.getBase(), clientDir.resolve("libraries"), e)).thenAccept((v) -> {
            LogHelper.subInfo("Client libraries successfully downloaded: '%s'", dirName);
        });
        IOHelper.transfer(IOHelper.newInput(new URL(info.client.url)), clientDir.resolve("minecraft.jar"));
        LogHelper.subInfo("Downloaded client jar");
        e.awaitTermination(4, TimeUnit.HOURS);
        e.shutdown();
        LogHelper.subInfo("Downloading natives!");
        fetchNatives(clientDir.resolve("natives"), info.natives);
        LogHelper.subInfo("Natives downloaded!");
        LogHelper.subInfo("Client downloaded!");
        // Finished
        server.syncUpdatesDir(Collections.singleton(dirName));
    }

	private void fetchNatives(Path resolve, List<Artifact> natives) {
		for (Artifact a : natives) {
			try (ZipInputStream z = IOHelper.newZipInput(new URL(a.name))) {
                ZipEntry e = z.getNextEntry();
                while (e != null) {
                	if (!e.isDirectory() && !e.getName().startsWith("META-INF") && !e.getName().startsWith("/META-INF")) {
                		IOHelper.transfer(z, resolve.resolve(e.getName()));
                	}
                	e = z.getNextEntry(); 
                }
			} catch (Throwable e) {
				LogHelper.subWarning(LogHelper.toString(e));
			}
		}
	}
}
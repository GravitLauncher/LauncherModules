package pro.gravit.launchermodules.unsafecommands.commands;

import com.google.gson.JsonObject;
import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchermodules.unsafecommands.impl.ClientDownloader;
import pro.gravit.launchermodules.unsafecommands.impl.ClientDownloader.Artifact;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.command.hash.SaveProfilesCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FetchClientCommand extends Command {
    public FetchClientCommand(LaunchServer server) {
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
        JsonObject obj;
        if (Files.exists(Paths.get(version))) {
            LogHelper.subInfo("Using file %s", version);
            try (Reader reader = IOHelper.newReader(Paths.get(version))) {
                obj = ClientDownloader.GSON.fromJson(reader, JsonObject.class);
            }
        } else {
            obj = ClientDownloader.gainClient(version);
        }
        ClientDownloader.ClientInfo info = ClientDownloader.getClient(obj);
        // Download required files
        LogHelper.subInfo("Downloading client, it may take some time");
        AsyncDownloader d = new AsyncDownloader();
        ExecutorService e = Executors.newFixedThreadPool(4);
        List<AsyncDownloader.SizedFile> applies = info.libraries.stream().map(y -> new AsyncDownloader.SizedFile(y.url, y.path, y.size)).collect(Collectors.toList());
        CompletableFuture<Void> f = CompletableFuture.allOf(d.runDownloadListSimple(d.sortFiles(applies, 4), "", clientDir.resolve("libraries"), e)).thenAccept((v) -> LogHelper.subInfo("Client libraries successfully downloaded!"));
        if (info.client != null) {
            IOHelper.transfer(IOHelper.newInput(new URL(info.client.url)), clientDir.resolve("minecraft.jar"));
        }
        LogHelper.subInfo("Downloaded client jar!");
        fetchNatives(clientDir.resolve("natives"), info.natives);
        LogHelper.subInfo("Natives downloaded!");
        f.get();
        e.shutdownNow();
        // Finished
        LogHelper.subInfo("Client downloaded!");
        server.syncUpdatesDir(Collections.singleton(dirName));
        try {
            ClientProfile.Version clientVersion = ClientProfile.Version.byName(version);
            SaveProfilesCommand.MakeProfileOption[] options = SaveProfilesCommand.getMakeProfileOptionsFromDir(clientDir, clientVersion);
            ClientProfile profile = SaveProfilesCommand.makeProfile(clientVersion, dirName, options);
            try (Writer w = IOHelper.newWriter(server.profilesDir.resolve(dirName + ".json"))) {
                Launcher.gsonManager.configGson.toJson(profile, w);
            }
            server.syncProfilesDir();
        } catch (Exception ex) {
            LogHelper.error(ex);
        }
    }

    private void fetchNatives(Path resolve, List<Artifact> natives) {
        for (Artifact a : natives) {
            try (ZipInputStream z = IOHelper.newZipInput(new URL(a.url))) {
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
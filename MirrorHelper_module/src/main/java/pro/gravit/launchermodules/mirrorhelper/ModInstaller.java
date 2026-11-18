package pro.gravit.launchermodules.mirrorhelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchermodules.mirrorhelper.modapi.CurseforgeAPI;
import pro.gravit.launchermodules.mirrorhelper.modapi.ModrinthAPI;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public final class ModInstaller {
    private static final Logger logger = LogManager.getLogger(ModInstaller.class);

    private ModInstaller() {}

    public static void install(String apiKey, Path modsDir, String loaderName,
                               ClientProfile.Version version, List<String> mods) throws InterruptedException {
        ModrinthAPI modrinthAPI = null;
        CurseforgeAPI curseforgeApi = null;
        for (var modId : mods) {
            try {
                try {
                    long id = Long.parseLong(modId);
                    if (curseforgeApi == null) {
                        curseforgeApi = new CurseforgeAPI(apiKey);
                    }
                    installMod(curseforgeApi, modsDir, id, version);
                    continue;
                } catch (NumberFormatException ignored) {
                }
                if (modrinthAPI == null) {
                    modrinthAPI = new ModrinthAPI();
                }
                installMod(modrinthAPI, modsDir, modId, loaderName, version);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                logger.warn("Mod {} could not be installed", modId, e);
            }
        }
    }

    private static void installMod(CurseforgeAPI api, Path modsDir, long modId, ClientProfile.Version version) throws Exception {
        var modInfo = api.fetchModById(modId);
        long fileId = modInfo.findFileIdByGameVersion(version.toString());
        var fileInfo = api.fetchModFileById(modId, fileId);
        URI url = WorkspaceManifest.remoteUri(fileInfo.downloadUrl());
        Path path = WorkspaceManifest.resolve(modsDir, WorkspaceManifest.component(fileInfo.fileName().replace("+", "-")));
        logger.info("Download {} {} into {}", fileInfo.fileName(), url, path);
        Downloader.downloadFile(url, path, null).getFuture().get();
        logger.info("{} downloaded", fileInfo.fileName());
    }

    private static void installMod(ModrinthAPI api, Path modsDir, String slug, String loader, ClientProfile.Version version) throws Exception {
        if (!slug.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid Modrinth project identifier: " + slug);
        }
        var list = api.getMod(slug);
        var mod = api.getModByGameVersion(list, version.toString(), loader);
        if(mod == null) {
            throw new RuntimeException("Mod '%s' not supported game version '%s'".formatted(slug, version.toString()));
        }
        ModrinthAPI.ModVersionFileData file = null;
        for(var e : mod.files()) {
            file = e;
            if(e.primary()) {
                break;
            }
        }
        if(file == null) {
            throw new RuntimeException("Mod '%s' not found suitable file".formatted(slug));
        }
        URI url = WorkspaceManifest.remoteUri(file.url());
        Path path = WorkspaceManifest.resolve(modsDir, WorkspaceManifest.component(file.filename().replace("+", "-")));
        logger.info("Download {} {} into {}", file.filename(), url, path);
        Downloader.downloadFile(url, path, null).getFuture().get();
        logger.info("{} downloaded", file.filename());
    }

}

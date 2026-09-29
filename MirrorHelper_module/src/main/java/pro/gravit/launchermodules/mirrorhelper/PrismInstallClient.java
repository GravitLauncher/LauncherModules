package pro.gravit.launchermodules.mirrorhelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launchermodules.mirrorhelper.newforge.ProfileModifier;
import pro.gravit.launchserver.command.profiles.CreateProfileCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;

import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PrismInstallClient extends InstallClient {
    private final String prismClientName;

    public static Path getPrismBaseDir() {
        if(JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            return Path.of(System.getenv("HOME")).resolve(".local").resolve("share").resolve("PrismLauncher");
        }
        throw new UnsupportedOperationException(String.format("Unsupported OS %s", JVMHelper.OS_TYPE.name));
    }

    public PrismInstallClient(MirrorHelperModule module, String name, ClientProfile.Version version, List<String> mods, VersionType versionType, MirrorWorkspace mirrorWorkspace, String prismClientName) {
        super(module, name, version, mods, versionType, mirrorWorkspace);
        this.prismClientName = prismClientName;
    }

    public static ExtendedClientInfo getPrismClient(JsonObject obj) {
        JsonArray libraries = obj.getAsJsonArray("libraries");
        ExtendedClientInfo ret = new ExtendedClientInfo();
        if(obj.has("order")) {
            ret.order = obj.get("order").getAsInt();
        }
        if(obj.has("uid")) {
            ret.uid = obj.get("uid").getAsString();
        }
        if(obj.has("version")) {
            ret.version = obj.get("version").getAsString();
        }
        if(obj.has("mainClass")) {
            ret.mainClass = obj.get("mainClass").getAsString();
        }
        if(obj.has("minecraftArguments")) {
            ret.mainClass = obj.get("minecraftArguments").getAsString();
        }
        for (JsonElement e : libraries) {
            if (e.isJsonObject() && e.getAsJsonObject().has("downloads")) {
                JsonObject downloads = e.getAsJsonObject().getAsJsonObject("downloads");
                if (downloads.has("classifiers")) {
                    JsonObject u = downloads.getAsJsonObject("classifiers");
                    u.entrySet().forEach(p -> {
                        if (p.getValue().isJsonObject() && p.getKey().startsWith("native")) {
                            ClientDownloader.Artifact a = ClientDownloader.GSON.fromJson(p.getValue(), ClientDownloader.Artifact.class);
                            a.name = p.getKey() + '/' + e.getAsJsonObject().get("name").getAsString();
                            ret.natives.add(a);
                        }
                    });
                } else if (downloads.has("artifact")) {
                    ClientDownloader.Artifact a = ClientDownloader.GSON.fromJson(downloads.get("artifact"), ClientDownloader.Artifact.class);
                    a.name = "art/" + e.getAsJsonObject().get("name").getAsString();
                    ret.libraries.add(a);
                }

            }
        }
        if (obj.has("downloads")) {
            JsonObject tmp = obj.getAsJsonObject("downloads");
            ret.client = ClientDownloader.GSON.fromJson(tmp.get("client"), ClientDownloader.Downloadable.class);
            ret.server = ClientDownloader.GSON.fromJson(tmp.get("server"), ClientDownloader.Downloadable.class);
        }
        //dedupe(ret.libraries);
        //dedupe(ret.natives);
        return ret;
    }

    @Override
    public void run() throws Exception {
        Path prismBaseDir = getPrismBaseDir();
        Path instanceDir = prismBaseDir.resolve("instances").resolve(prismClientName);
        if(Files.notExists(instanceDir)) {
            throw new FileNotFoundException(instanceDir.toString());
        }
        Path instanceMinecraftDir = instanceDir.resolve("minecraft");
        Path metaDir = prismBaseDir.resolve("meta");
        logger.info("Read PrismLauncher instance information");
        MmcPackData data;
        try(Reader reader = IOHelper.newReader(instanceDir.resolve("mmc-pack.json"))) {
            data = Launcher.gsonManager.configGson.fromJson(reader, MmcPackData.class);
        }
        List<ExtendedClientInfo> list = new ArrayList<>(data.components.size());
        for(var component : data.components) {
            Path jsonFile = metaDir.resolve(component.uid).resolve(component.version+".json");
            try(Reader reader = IOHelper.newReader(jsonFile)) {
                list.add(getPrismClient(ClientDownloader.GSON.fromJson(reader, JsonObject.class)));
            }
        }
        list.sort(Comparator.comparingInt(e -> e.order));
        ExtendedClientInfo merged = new ExtendedClientInfo();
        {
            Map<String, ClientDownloader.Artifact> libraries = new HashMap<>();
            Map<String, ClientDownloader.Artifact> natives = new HashMap<>();
            for(var c : list) {
                if(c.mainClass != null) {
                    merged.mainClass = c.mainClass;
                }
                if(c.minecraftArguments != null) {
                    merged.minecraftArguments = c.minecraftArguments;
                }
                for(var l : c.libraries) {
                    libraries.put(l.name, l);
                }
                for(var n : c.natives) {
                    natives.put(n.name, n);
                }
            }
            merged.natives = new ArrayList<>(natives.values());
            merged.libraries = new ArrayList<>(libraries.values());
        }
        Path clientPath = launchServer.createTempDirectory(name);
        checkAndDownloadVanilla(clientPath);
        logger.info("Copy {} into {}", instanceMinecraftDir, clientPath);
        copyDir(instanceMinecraftDir, clientPath);
        logger.info("Copy libraries");
        Path prismLibrariesDir = prismBaseDir.resolve("libraries");
        for(var l : merged.libraries) {
            if(l.path == null) {
                logger.warn("Library path is null {}", l.name);
                continue;
            }
            Path sourceFile = prismLibrariesDir.resolve(l.path);
            if(Files.notExists(sourceFile)) {
                logger.warn("Skip {}: {} not found", l.name, sourceFile);
            }
            Path targetFile = clientPath.resolve("libraries").resolve(l.path);
            logger.debug("Copy {} into {}", sourceFile, targetFile);
            IOHelper.createParentDirs(targetFile);
            IOHelper.copy(sourceFile, targetFile);
        }
        logger.info("Patch authlib");
        Path tmpFile = workdir.resolve("file.tmp");
        patchAuthlib(clientPath, tmpFile);
        logger.info("Copy common directories");
        String lwjgl3Version = getLwjgl3Version();
        copyCommonDirs(clientPath, lwjgl3Version);
        installModsFromInternet(clientPath);
        logger.info("Install multiMods");
        installMultimods(clientPath);
        runDedupLibraries(clientPath);

        ClientProfileBuilder clientProfileBuilder = new ClientProfileBuilder(makeClientProfile(clientPath));
        {
            clientProfileBuilder.setMainClass(merged.mainClass);
            {
                List<String> args = new ArrayList<>();
                String prev = null;
                for(var e : merged.minecraftArguments.split(" ")) {
                    if(prev == null) {
                        prev = e;
                        continue;
                    }
                    if(e.startsWith("${")) { // Default minecraft placeholders, skip it
                        prev = null;
                        continue;
                    }
                    args.add(prev);
                    args.add(e);
                }
                if(prev != null) {
                    args.add(prev);
                }
                clientProfileBuilder.setClientArgs(args);
            }
            clientProfileBuilder.setClassPath(new ArrayList<>());
            {
                for(var l : merged.libraries) {
                    clientProfileBuilder.classPath("libraries/"+l.path);
                }
            }
        }
        CreateProfileCommand.pushClientAndDownloadAssets(launchServer, clientProfileBuilder.createClientProfile(), clientPath, !config.disableDownloadAssets);
        logger.info("Completed");
    }

    public static class ExtendedClientInfo extends ClientDownloader.ClientInfo {
        public Integer order;
        public String uid;
        public String version;
        public String mainClass;
        public String minecraftArguments;
    }

    public record MmcPackData(List<MmcPackComponent> components) {

    }

    public record MmcPackComponent(String uid, String version, boolean important) {

    }


}

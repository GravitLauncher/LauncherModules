package pro.gravit.launchermodules.mirrorhelper;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileVersions;
import pro.gravit.launchermodules.mirrorhelper.commands.DeDupLibrariesCommand;
import pro.gravit.launchermodules.mirrorhelper.installers.FabricInstallerCommand;
import pro.gravit.launchermodules.mirrorhelper.installers.QuiltInstallerCommand;
import pro.gravit.launchermodules.mirrorhelper.modapi.CurseforgeAPI;
import pro.gravit.launchermodules.mirrorhelper.modapi.ModrinthAPI;
import pro.gravit.launchermodules.mirrorhelper.newforge.CleanroomProfileModifier;
import pro.gravit.launchermodules.mirrorhelper.newforge.ForgeProfile;
import pro.gravit.launchermodules.mirrorhelper.newforge.ForgeProfileModifier;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.profiles.MakeProfileCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class InstallClient {
    private static final Logger logger = LogManager.getLogger();
    private final LaunchServer launchServer;
    private final Config config;
    private final Path workdir;
    private final String name;
    private final ClientProfile.Version version;
    private final WorkspaceTools tools;

    private final List<String> mods;
    private final MirrorWorkspace mirrorWorkspace;
    private final VersionType versionType;

    public InstallClient(MirrorHelperModule module, String name, ClientProfile.Version version, List<String> mods, VersionType versionType, MirrorWorkspace mirrorWorkspace) {
        this.launchServer = module.server;
        this.config = module.config;
        this.workdir = module.getWorkspaceDir();
        this.tools = module.tools;
        this.name = name;
        this.version = version;
        this.mods = mods;
        this.versionType = versionType;
        this.mirrorWorkspace = mirrorWorkspace;
    }

    public static void installMod(CurseforgeAPI api, Path modsDir, long modId, ClientProfile.Version version) throws Exception {
        var modInfo = api.fetchModById(modId);
        long fileId = modInfo.findFileIdByGameVersion(version.toString());
        var fileInfo = api.fetchModFileById(modId, fileId);
        URI url = new URI(fileInfo.downloadUrl());
        Path path = modsDir.resolve(fileInfo.fileName().replace("+", "-"));
        logger.info("Download {} {} into {}", fileInfo.fileName(), url, path);
        try (InputStream input = IOHelper.newInput(url.toURL())) {
            IOHelper.transfer(input, path);
        }
        logger.info("{} downloaded", fileInfo.fileName());
    }

    public static void installMod(ModrinthAPI api, Path modsDir, String slug, String loader, ClientProfile.Version version) throws Exception {
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
        URI url = new URI(file.url());
        Path path = modsDir.resolve(file.filename().replace("+", "-"));
        logger.info("Download {} {} into {}", file.filename(), url, path);
        try (InputStream input = IOHelper.newInput(url.toURL())) {
            IOHelper.transfer(input, path);
        }
        logger.info("{} downloaded", file.filename());
    }

    private void downloadVanillaTo(Path clientDir) throws Exception {
        JsonObject obj;
        Path vanillaProfileJson = workdir.resolve("profiles").resolve("vanilla").resolve(version.toString().concat(".json"));
        if (Files.exists(vanillaProfileJson)) {
            LogHelper.subInfo("Using file %s", vanillaProfileJson);
            try (Reader reader = IOHelper.newReader(vanillaProfileJson)) {
                obj = ClientDownloader.GSON.fromJson(reader, JsonObject.class);
            }
        } else {
            IOHelper.createParentDirs(vanillaProfileJson);
            obj = ClientDownloader.gainClient(version.toString());
            try (Writer writer = IOHelper.newWriter(vanillaProfileJson)) {
                Launcher.gsonManager.configGson.toJson(obj, writer);
            }
        }
        IOHelper.createParentDirs(clientDir);
        ClientDownloader.ClientInfo info = ClientDownloader.getClient(obj);
        // Download required files
        LogHelper.subInfo("Downloading client, it may take some time");
        ExecutorService e = Executors.newFixedThreadPool(4);
        //info.libraries.addAll(info.natives); // Hack
        List<Downloader.SizedFile> applies = info.libraries.stream()
                .filter(l -> !(l.name.contains("natives")))
                .map(y -> new Downloader.SizedFile(y.url, y.path, y.size)).collect(Collectors.toList());
        var downloader = Downloader.downloadList(applies, null, clientDir.resolve("libraries"), null, e, 4);
        if (info.client != null) {
            IOHelper.transfer(IOHelper.newInput(new URI(info.client.url).toURL()), clientDir.resolve("minecraft.jar"));
        }
        LogHelper.subInfo("Downloaded client jar!");
        downloader.getFuture().get();
        e.shutdownNow();
        // Finished
        LogHelper.subInfo("Client downloaded!");
    }

    private void fetchNatives(Path resolve, List<ClientDownloader.Artifact> natives) {
        for (ClientDownloader.Artifact a : natives) {
            try (ZipInputStream z = IOHelper.newZipInput(new URI(a.url).toURL())) {
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

    public void run() throws Exception {
        if(mirrorWorkspace == null) {
            throw new RuntimeException("Workspace not found! Please use 'applyworkspace'");
        }
        logger.info("Install client {} {}", version.toString(), versionType);
        Path originalMinecraftProfile = null;
        Path clientPath = launchServer.updatesDir.resolve(name);
        {
            Path fetchDir = workdir.resolve("clients").resolve("vanilla").resolve(version.toString());
            if (Files.notExists(fetchDir)) {
                downloadVanillaTo(fetchDir);
            }
            copyDir(fetchDir, clientPath, path -> !(
                    path.toString().contains("icu4j-core-mojang") &&
                            versionType == VersionType.FORGE &&
                            version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) == 0
            ));
        }
        Path tmpFile = workdir.resolve("file.tmp");
        {
            Path pathToLauncherAuthlib = getPathToLauncherAuthlib();
            logger.info("Found launcher authlib in {}", pathToLauncherAuthlib);
            Path pathToOriginalAuthlib = findClientAuthlib(clientPath);
            logger.info("Found original authlib in {}", pathToOriginalAuthlib);
            merge2Jars(pathToOriginalAuthlib, pathToLauncherAuthlib, tmpFile);
            Files.delete(pathToOriginalAuthlib);
            Files.move(tmpFile, pathToOriginalAuthlib);
            logger.info("Authlib patched");
        }
        {
            if (versionType == VersionType.FABRIC) {
                FabricInstallerCommand fabricInstallerCommand = new FabricInstallerCommand(launchServer);
                if(mirrorWorkspace == null || mirrorWorkspace.fabricLoaderVersion() == null) {
                    fabricInstallerCommand.invoke(version.toString(), name, workdir.resolve("installers").resolve("fabric-installer.jar").toAbsolutePath().toString());
                } else {
                    fabricInstallerCommand.invoke(version.toString(), name, workdir.resolve("installers").resolve("fabric-installer.jar").toAbsolutePath().toString(), mirrorWorkspace.fabricLoaderVersion());
                }
                Files.createDirectories(clientPath.resolve("mods"));
                logger.info("Fabric installed");
            } else if (versionType == VersionType.QUILT) {
                QuiltInstallerCommand quiltInstallerCommand = new QuiltInstallerCommand(launchServer);
                quiltInstallerCommand.invoke(version.toString(), name, workdir.resolve("installers").resolve("quilt-installer.jar").toAbsolutePath().toString());
                Files.createDirectories(clientPath.resolve("mods"));
                logger.info("Quilt installed");
            } else if (versionType == VersionType.FORGE || versionType == VersionType.NEOFORGE) {
                String forgePrefix = versionType == VersionType.NEOFORGE ? "neoforge" : "forge";
                boolean noGui = true;
                Path forgeInstaller = workdir.resolve("installers").resolve(forgePrefix+"-" + version + "-installer-nogui.jar");
                Path tmpDir = workdir.resolve("clients").resolve(forgePrefix).resolve(version.toString());
                if(Files.notExists(forgeInstaller)) {
                    logger.warn("{} not found", forgeInstaller.toAbsolutePath().toString());
                    forgeInstaller = workdir.resolve("installers").resolve(forgePrefix+"-" + version + "-installer.jar");
                    noGui = false;
                }
                if(Files.notExists(forgeInstaller) && Files.notExists(tmpDir)) {
                    throw new FileNotFoundException(forgeInstaller.toAbsolutePath().toString());
                }
                if(Files.notExists(tmpDir)) {
                    Files.createDirectories(tmpDir);
                    Files.createDirectories(tmpDir.resolve("versions"));
                    IOHelper.transfer("{\"profiles\": {}}".getBytes(StandardCharsets.UTF_8), tmpDir.resolve("launcher_profiles.json"), false);
                    int counter = 5;
                    do {
                        Process forgeProcess;
                        if(noGui) {
                            logger.info("Install forge client into {} (no gui)", tmpDir.toAbsolutePath().toString());
                            forgeProcess = new ProcessBuilder()
                                    .command("java", "-jar", forgeInstaller.toAbsolutePath().toString(), "--installClient", tmpDir.toAbsolutePath().toString())
                                    .inheritIO()
                                    .start();
                        } else {
                            logger.info("Please install forge client into {} (require gui)", tmpDir.toAbsolutePath().toString());
                            forgeProcess = new ProcessBuilder()
                                    .command("java", "-jar", forgeInstaller.toAbsolutePath().toString())
                                    .inheritIO()
                                    .start();
                        }
                        int code = forgeProcess.waitFor();
                        logger.info("Process return with status code {}", code);
                        counter--;
                        if (counter <= 0) {
                            IOHelper.deleteDir(tmpDir, true);
                            throw new RuntimeException("Forge not installed");
                        }
                    } while (!Files.isDirectory(tmpDir.resolve("libraries")));
                }
                copyDir(tmpDir.resolve("libraries"), clientPath.resolve("libraries"));
                {
                    Path forgeClientDir;
                    try (Stream<Path> stream = Files.list(tmpDir.resolve("versions"))
                                 .filter(x -> {
                                     String fname = x.getFileName().toString().toLowerCase(Locale.ROOT);
                                     return  fname.contains("forge") || fname.contains("cleanroom");
                                 })) {
                        forgeClientDir = stream.findFirst().orElseThrow();
                    }
                    Path forgeProfileFile;
                    try(Stream<Path> stream = Files.list(forgeClientDir).filter(p -> p.getFileName().toString().endsWith(".json"))) {
                        forgeProfileFile = stream.findFirst().orElseThrow();
                    }
                    originalMinecraftProfile = forgeProfileFile;
                    logger.debug("Forge profile {}", forgeProfileFile.toString());
                    ForgeProfile forgeProfile;
                    try (Reader reader = IOHelper.newReader(forgeProfileFile)) {
                        forgeProfile = Launcher.gsonManager.configGson.fromJson(reader, ForgeProfile.class);
                    }
                    for (ForgeProfile.ForgeProfileLibrary library : forgeProfile.libraries()) {
                        String libUrl = library.downloads() == null ? null : library.downloads().artifact().url();
                        String name = library.name();
                        if (libUrl == null || libUrl.isEmpty()) {
                            libUrl = "https://libraries.minecraft.net/";
                        }
                        if(name.endsWith("@jar")) {
                            name = name.substring(0, name.length()-4);
                        }
                        FabricInstallerCommand.NamedURL url = FabricInstallerCommand.makeURL(libUrl, name);
                        Path file = clientPath.resolve("libraries").resolve(url.name);
                        IOHelper.createParentDirs(file);
                        if (Files.exists(file)) {
                            continue;
                        }
                        logger.info("Download {} into {}", url.url.toString(), url.name);
                        try {
                            try (InputStream stream = IOHelper.newInput(url.url)) {
                                try (OutputStream output = IOHelper.newOutput(file)) {
                                    IOHelper.transfer(stream, output);
                                }
                            }
                        } catch (FileNotFoundException e) {
                            LogHelper.warning("Not found %s", url.url);
                        }
                    }
                }
                if(config.deleteTmpDir) {
                    IOHelper.deleteDir(tmpDir, true);
                }
                Files.createDirectories(clientPath.resolve("mods"));
                logger.info("Forge installed");
            }
        }
        {
            for(var entry : mirrorWorkspace.build().entrySet()) {
                var k = entry.getKey();
                var v = entry.getValue();
                if(!v.check(versionType, version)) {
                    continue;
                }
                Path target = workdir.resolve(v.path());
                if(entry.getValue().dynamic() || Files.notExists(target)) {
                    logger.info("Build {}", k);
                    try {
                        tools.build(k, v, clientPath);
                    } catch (Throwable e) {
                        logger.error("Build error", e);
                    }
                }
            }
        }
        logger.info("Build required libraries");
        String lwjgl3Version = mirrorWorkspace.lwjgl3version();
        for(var e : mirrorWorkspace.lwjglVersionOverride()) {
            if(version.compareTo(e.minVersion()) < 0) {
                continue;
            }
            if(version.compareTo(e.maxVersion()) > 0) {
                continue;
            }
            lwjgl3Version = e.value();
        }
        {
            copyDir(workdir.resolve("workdir").resolve("ALL"), clientPath);
            copyDir(workdir.resolve("workdir").resolve(versionType.name()), clientPath);
            copyDir(workdir.resolve("workdir").resolve("lwjgl").resolve(lwjgl3Version), clientPath);
            copyDir(workdir.resolve("workdir").resolve("java17"), clientPath);
            copyDir(workdir.resolve("workdir").resolve(version.toString()).resolve("ALL"), clientPath);
            copyDir(workdir.resolve("workdir").resolve(version.toString()).resolve(versionType.name()), clientPath);
            logger.info("Files copied");
        }
        if (mods != null && !mods.isEmpty()) {
            ModrinthAPI modrinthAPI = null;
            CurseforgeAPI curseforgeApi = null;
            Path modsDir = clientPath.resolve("mods");
            String loaderName = switch (versionType) {
                case VANILLA -> "";
                case FABRIC -> "fabric";
                case NEOFORGE -> "neoforge";
                case FORGE -> "forge";
                case QUILT -> "quilt";
            };
            for (var modId : mods) {
                try {
                    try {
                        long id = Long.parseLong(modId);
                        if (curseforgeApi == null) {
                            curseforgeApi = new CurseforgeAPI(config.curseforgeApiKey);
                        }
                        installMod(curseforgeApi, modsDir, id, version);
                        continue;
                    } catch (NumberFormatException ignored) {
                    }
                    if (modrinthAPI == null) {
                        modrinthAPI = new ModrinthAPI();
                    }
                    installMod(modrinthAPI, modsDir, modId, loaderName, version);
                } catch (Throwable e) {
                    logger.warn("Mod {} not installed! Exception {}", modId, e);
                }
            }
            logger.info("Mods installed");
        }
        logger.info("Install multiMods");
        for (var m : mirrorWorkspace.multiMods().entrySet()) {
            var k = m.getKey();
            var v = m.getValue();
            if(!v.check(versionType, version)) {
                continue;
            }
            Path file = workdir.resolve("multimods").resolve(k.concat(".jar"));
            if (Files.notExists(file)) {
                logger.warn("File {} not exist", file);
                continue;
            }
            Path targetMod = v.target() != null ? clientPath.resolve(v.target()) : clientPath.resolve("mods").resolve(file.getFileName());
            logger.info("Copy {} to {}", file, targetMod);
            IOHelper.copy(file, targetMod);
            logger.info("MultiMods installed");
        }
        {
            DeDupLibrariesCommand deDupLibrariesCommand = new DeDupLibrariesCommand(launchServer);
            deDupLibrariesCommand.invoke(clientPath.toAbsolutePath().toString(), "false");
            logger.info("deduplibraries completed");
        }
        {
            MakeProfileCommand makeProfileCommand = new MakeProfileCommand(launchServer);
            makeProfileCommand.invoke(name, version.toString(), name);
            logger.info("makeprofile completed");
        }
        if((versionType == VersionType.FORGE || versionType == VersionType.NEOFORGE) && version.compareTo(ClientProfileVersions.MINECRAFT_1_17) >= 0) {
            ClientProfile profile = launchServer.config.profileProvider.getProfile(name);
            logger.info("Run ForgeProfileModifier");
            ForgeProfileModifier modifier = new ForgeProfileModifier(originalMinecraftProfile, profile, clientPath);
            profile = modifier.build();
            launchServer.config.profileProvider.addProfile(profile);
        }
        if (versionType == VersionType.FORGE && version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) == 0) {
            ClientProfile profile = launchServer.config.profileProvider.getProfile(name);
            logger.info("Run CleanroomProfileModifier");
            CleanroomProfileModifier modifier = new CleanroomProfileModifier(originalMinecraftProfile, profile, clientPath);
            profile = modifier.build();
            launchServer.config.profileProvider.addProfile(profile);
        }
        launchServer.syncUpdatesDir(Collections.singleton(name));
        logger.info("Completed");
    }

    private Path getPathToLauncherAuthlib() {
        Path pathToLauncherAuthlib;
        if (version.compareTo(ClientProfileVersions.MINECRAFT_1_16_5) < 0) {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib1.jar");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_18) < 0) {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib2.jar");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_19) < 0) {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib3.jar");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_19) == 0) {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib3-1.19.jar");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_20) < 0) {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib3-1.19.1.jar");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_20_2) < 0)  {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib4.jar");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_20_3) < 0)  {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib5.jar");
        } else  {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib6.jar");
        }
        return pathToLauncherAuthlib;
    }

    private void copyDir(Path source, Path target) throws IOException {
        copyDir(source, target, path -> true);
    }
    private void copyDir(Path source, Path target, Predicate<Path> predicate) throws IOException {
        if (Files.notExists(source)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(e -> predicate.test(source.relativize(e))).forEach(src -> {
                try {
                    Path dest = target.resolve(source.relativize(src));
                    if (Files.isDirectory(src)) {
                        if (Files.notExists(dest)) {
                            Files.createDirectories(dest);
                        }
                    } else {
                        IOHelper.copy(src, target.resolve(source.relativize(src)));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private Path findClientAuthlib(Path clientDir) throws IOException {
        try (Stream<Path> stream = Files.walk(clientDir).filter(p -> !Files.isDirectory(p) && p.getFileName().toString().startsWith("authlib-"))) {
            return stream.findFirst().orElseThrow();
        }
    }

    private void merge2Jars(Path source, Path source2, Path target) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(target))) {
            Set<String> blacklist = new HashSet<>();
            try (ZipInputStream input = IOHelper.newZipInput(source2)) {
                ZipEntry e = input.getNextEntry();
                while (e != null) {
                    if (e.getName().startsWith("META-INF")) {
                        e = input.getNextEntry();
                        continue;
                    }
                    blacklist.add(e.getName());
                    ZipEntry newEntry = IOHelper.newZipEntry(e);
                    output.putNextEntry(newEntry);
                    IOHelper.transfer(input, output);
                    e = input.getNextEntry();
                }
            }
            try (ZipInputStream input = IOHelper.newZipInput(source)) {
                ZipEntry e = input.getNextEntry();
                while (e != null) {
                    if (blacklist.contains(e.getName())) {
                        e = input.getNextEntry();
                        continue;
                    }
                    blacklist.add(e.getName());
                    ZipEntry newEntry = IOHelper.newZipEntry(e);
                    output.putNextEntry(newEntry);
                    IOHelper.transfer(input, output);
                    e = input.getNextEntry();
                }
            }
        }
    }

    public enum VersionType {
        VANILLA, FABRIC, NEOFORGE, FORGE, QUILT
    }
}

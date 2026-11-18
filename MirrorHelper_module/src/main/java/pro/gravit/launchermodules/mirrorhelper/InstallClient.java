package pro.gravit.launchermodules.mirrorhelper;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.Downloader;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileVersions;
import pro.gravit.launchermodules.mirrorhelper.commands.DeDupLibrariesCommand;
import pro.gravit.launchermodules.mirrorhelper.installers.FabricInstallerCommand;
import pro.gravit.launchermodules.mirrorhelper.installers.QuiltInstallerCommand;
import pro.gravit.launchermodules.mirrorhelper.installers.LoaderLibraries;
import pro.gravit.launchermodules.mirrorhelper.newforge.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.command.profiles.CreateProfileCommand;
import pro.gravit.launchserver.helper.MakeProfileHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class InstallClient {
    static final Logger logger = LogManager.getLogger();
    final LaunchServer launchServer;
    final Config config;
    final Path workdir;
    final Path cacheDir;
    final Path installerDir;
    final String name;
    final ClientProfile.Version version;
    final WorkspaceBuildRunner builds;

    final List<String> mods;
    final MirrorWorkspace mirrorWorkspace;
    final VersionType versionType;

    public InstallClient(MirrorHelperModule module, String name, ClientProfile.Version version, List<String> mods, VersionType versionType, MirrorWorkspace mirrorWorkspace) {
        this.launchServer = module.server;
        this.config = module.config;
        this.workdir = module.getWorkspaceDir();
        this.cacheDir = module.getCacheDir();
        this.installerDir = module.getInstallerDir();
        this.builds = new WorkspaceBuildRunner(module);
        this.name = WorkspaceManifest.component(name);
        WorkspaceManifest.component(version.toString());
        this.version = version;
        this.mods = mods;
        this.versionType = versionType;
        this.mirrorWorkspace = mirrorWorkspace;
    }

    private void downloadVanillaTo(Path clientDir) throws Exception {
        JsonObject obj;
        Path vanillaProfileJson = cacheDir.resolve("profiles").resolve("vanilla").resolve(version.toString().concat(".json"));
        if (Files.exists(vanillaProfileJson)) {
            logger.info("Using file {}", vanillaProfileJson);
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
        logger.info("Downloading client, it may take some time");
        //info.libraries.addAll(info.natives); // Hack
        List<Downloader.SizedFile> applies = info.libraries.stream()
                .filter(l -> !(l.name.contains("natives")))
                .map(y -> new Downloader.SizedFile(y.url, safeLibraryPath(clientDir, y.path), y.size)).collect(Collectors.toList());
        applies.add(new Downloader.SizedFile(info.client.url, "minecraft.jar"));
        var downloader = Command.downloadWithProgressBar(version.toString(), applies, null, clientDir);
        downloader.getFuture().get();
        // Finished
        logger.info("Client downloaded!");
    }

    public final void run() throws Exception {
        Path clientPath = launchServer.createTempDirectory(name);
        try {
            checkAndDownloadVanilla(clientPath);
            ClientProfile profile = prepareProfile(clientPath);
            CreateProfileCommand.pushClientAndDownloadAssets(launchServer, profile, clientPath, !config.disableDownloadAssets);
            logger.info("Completed");
        } finally {
            if (Files.exists(clientPath)) {
                IOHelper.deleteDir(clientPath, true);
            }
        }
    }

    ClientProfile prepareProfile(Path clientPath) throws Exception {
        patchAuthlib(clientPath);
        Path loaderProfile = installModloader(clientPath, null);
        try {
            applyClientWorkspace(clientPath);
            return getProfileModifier(loaderProfile, makeClientProfile(clientPath), clientPath).build();
        } finally {
            if (config.deleteTmpDir && loaderProfile != null) {
                IOHelper.deleteDir(loaderProfile.getParent().getParent().getParent(), true);
            }
        }
    }

    @NonNull ProfileModifier getProfileModifier(Path originalMinecraftProfile, ClientProfile clientProfile, Path clientPath) {
        ProfileModifier modifier;
        if((versionType == VersionType.FORGE || versionType == VersionType.NEOFORGE) && version.compareTo(ClientProfileVersions.MINECRAFT_1_17) >= 0) {
            logger.info("Run Forge118ProfileModifier");
            modifier = new Forge118ProfileModifier(originalMinecraftProfile, clientProfile, clientPath);
        } else if (versionType == VersionType.FORGE && version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) == 0) {
            logger.info("Run CleanroomProfileModifier");
            modifier = new CleanroomProfileModifier(originalMinecraftProfile, clientProfile, clientPath);
        } else if (versionType == VersionType.FORGE && version.compareTo(ClientProfileVersions.MINECRAFT_1_7_10) == 0) {
            logger.info("Run Lwjgl3ifyProfileModifier");
            modifier = new Lwjgl3ifyProfileModifier(clientProfile, clientPath);
        } else {
            logger.info("Run BasicProfileModifier");
            modifier = new BasicProfileModifier(clientProfile, clientPath);
        }
        return modifier;
    }

    ClientProfile makeClientProfile(Path clientPath) throws IOException {
        ClientProfile clientProfile;
        {
            MakeProfileHelper.MakeProfileOption[] options = MakeProfileHelper.getMakeProfileOptionsFromDir(clientPath, version);
            for (MakeProfileHelper.MakeProfileOption option : options) {
                logger.debug("Detected option {}", option.getClass().getSimpleName());
            }
            clientProfile = MakeProfileHelper.makeProfile(version, name, options);
            logger.info("makeprofile completed");
        }
        return clientProfile;
    }

    void runDedupLibraries(Path clientPath) throws Exception {
        DeDupLibrariesCommand deDupLibrariesCommand = new DeDupLibrariesCommand(launchServer);
        deDupLibrariesCommand.invoke(clientPath.toAbsolutePath().toString(), "false");
        logger.info("deduplibraries completed");
    }

    void installMultimods(Path clientPath) throws IOException {
        for (var m : mirrorWorkspace.clientArtifacts().entrySet()) {
            var k = m.getKey();
            var v = m.getValue();
            if(!v.check(versionType, version)) {
                continue;
            }
            Path file = workdir.resolve("multimods").resolve(k.concat(".jar"));
            if (Files.notExists(file)) {
                throw new FileNotFoundException(file.toString());
            }
            Path targetMod = WorkspaceManifest.resolve(clientPath,
                    v.target() != null ? v.target() : "mods/" + file.getFileName());
            logger.info("Copy {} to {}", file, targetMod);
            IOHelper.createParentDirs(targetMod);
            IOHelper.copy(file, targetMod);
            logger.info("MultiMods installed");
        }
    }

    void installModsFromInternet(Path clientPath) throws IOException, InterruptedException {
        String loader = versionType == VersionType.VANILLA ? "" : versionType.name().toLowerCase(Locale.ROOT);
        ModInstaller.install(config.curseforgeApiKey, WorkspaceManifest.resolve(clientPath, "mods"), loader, version, mods);
    }

    void copyCommonDirs(Path clientPath, String lwjgl3Version) throws IOException {
        copyDir(workdir.resolve("workdir").resolve("ALL"), clientPath);
        copyDir(workdir.resolve("workdir").resolve(versionType.name()), clientPath);
        copyDir(workdir.resolve("workdir").resolve("lwjgl").resolve(lwjgl3Version), clientPath);
        copyDir(workdir.resolve("workdir").resolve("java17"), clientPath);
        copyDir(workdir.resolve("workdir").resolve(version.toString()).resolve("ALL"), clientPath);
        copyDir(workdir.resolve("workdir").resolve(version.toString()).resolve(versionType.name()), clientPath);
        logger.info("Files copied");
    }

    void applyClientWorkspace(Path clientPath) throws Exception {
        for (var entry : mirrorWorkspace.builds().entrySet()) {
            var build = entry.getValue();
            if (build.check(versionType, version)
                    && (build.dynamic() || Files.notExists(WorkspaceManifest.resolve(workdir, build.path())))) {
                builds.build(entry.getKey(), build, clientPath);
            }
        }
        copyCommonDirs(clientPath, mirrorWorkspace.lwjglVersion(version));
        installModsFromInternet(clientPath);
        installMultimods(clientPath);
        runDedupLibraries(clientPath);
    }

    private static String safeLibraryPath(Path clientDir, String artifactPath) {
        try {
            return clientDir.relativize(WorkspaceManifest.resolve(clientDir, "libraries/" + artifactPath)).toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Path installModloader(Path clientPath, Path originalMinecraftProfile) throws Exception {
        if (versionType == VersionType.FABRIC) {
            FabricInstallerCommand.install(version.toString(), clientPath,
                    WorkspaceManifest.resolve(workdir, "installers/fabric-installer.jar"), mirrorWorkspace.loaderVersion(VersionType.FABRIC));
            Files.createDirectories(clientPath.resolve("mods"));
            logger.info("Fabric installed");
        } else if (versionType == VersionType.QUILT) {
            QuiltInstallerCommand.install(version.toString(), clientPath, WorkspaceManifest.resolve(workdir, "installers/quilt-installer.jar"));
            Files.createDirectories(clientPath.resolve("mods"));
            logger.info("Quilt installed");
        } else if (versionType == VersionType.FORGE || versionType == VersionType.NEOFORGE) {
            String forgePrefix = versionType == VersionType.NEOFORGE ? "neoforge" : "forge";
            String prefix = forgePrefix + "-" + version + "-installer";
            Path forgeInstaller = selectForgeInstaller(prefix);
            boolean noGui = forgeInstaller.getFileName().toString().endsWith("-nogui.jar");
            byte[] hash = SecurityHelper.digest(
                    SecurityHelper.DigestAlgorithm.SHA256, forgeInstaller);
            Path cache = cacheDir.resolve("clients").resolve(forgePrefix).resolve(version.toString())
                    .resolve(HexFormat.of().formatHex(hash));
            if (Files.notExists(cache)) {
                Files.createDirectories(cache.getParent());
                Path tmpDir = Files.createTempDirectory(cache.getParent(), "installer-");
                try {
                    Files.createDirectories(tmpDir.resolve("versions"));
                    IOHelper.transfer("{\"profiles\": {}}".getBytes(StandardCharsets.UTF_8),
                            tmpDir.resolve("launcher_profiles.json"), false);
                    List<String> command = new ArrayList<>(List.of(
                            IOHelper.resolveJavaBin(IOHelper.JVM_DIR).toString(), "-jar", forgeInstaller.toString()));
                    if (noGui) {
                        command.addAll(List.of("--installClient", tmpDir.toAbsolutePath().toString()));
                    } else {
                        logger.info("Install into {} using the installer window", tmpDir.toAbsolutePath());
                    }
                    Process process = new ProcessBuilder(command).inheritIO().start();
                    try {
                        if (process.waitFor() != 0 || !Files.isDirectory(tmpDir.resolve("libraries"))) {
                            throw new IOException("Installer failed: " + forgeInstaller);
                        }
                    } catch (InterruptedException e) {
                        process.destroyForcibly();
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                    findForgeProfile(tmpDir);
                    Files.move(tmpDir, cache, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    if (Files.exists(tmpDir)) {
                        IOHelper.deleteDir(tmpDir, true);
                    }
                }
            }
            Path tmpDir = cache;
            copyDir(tmpDir.resolve("libraries"), clientPath.resolve("libraries"));
            {
                Path forgeProfileFile = findForgeProfile(tmpDir);
                originalMinecraftProfile = forgeProfileFile;
                logger.debug("Forge profile {}", forgeProfileFile.toString());
                ForgeProfile forgeProfile;
                try (Reader reader = IOHelper.newReader(forgeProfileFile)) {
                    forgeProfile = Launcher.gsonManager.configGson.fromJson(reader, ForgeProfile.class);
                }
                for (ForgeProfile.ForgeProfileLibrary library : forgeProfile.libraries()) {
                    String libUrl = library.downloads() == null || library.downloads().artifact() == null
                            ? null : library.downloads().artifact().url();
                    String name = library.name();
                    if (libUrl == null || libUrl.isEmpty()) {
                        libUrl = "https://libraries.minecraft.net/";
                    }
                    if(name.endsWith("@jar")) {
                        name = name.substring(0, name.length()-4);
                    }
                    LoaderLibraries.Artifact artifact = LoaderLibraries.resolve(libUrl, name);
                    Path file = WorkspaceManifest.resolve(clientPath, "libraries/" + artifact.path());
                    IOHelper.createParentDirs(file);
                    if (Files.exists(file)) {
                        continue;
                    }
                    logger.info("Download {} into {}", artifact.uri(), artifact.path());
                    try {
                        try (InputStream stream = IOHelper.newInput(artifact.uri().toURL())) {
                            try (OutputStream output = IOHelper.newOutput(file)) {
                                IOHelper.transfer(stream, output);
                            }
                        }
                    } catch (FileNotFoundException e) {
                        logger.warn("Not found {}", artifact.uri().toURL());
                    }
                }
            }
            Files.createDirectories(clientPath.resolve("mods"));
            logger.info("Forge installed");
        }
        return originalMinecraftProfile;
    }

    private Path selectForgeInstaller(String prefix) throws IOException {
        for (Path directory : List.of(installerDir, workdir.resolve("installers"))) {
            for (String suffix : List.of("-nogui.jar", ".jar")) {
                Path installer = WorkspaceManifest.resolve(directory, prefix + suffix);
                if (Files.isRegularFile(installer)) {
                    return installer.toAbsolutePath();
                }
            }
        }
        throw new FileNotFoundException("No installer found for " + prefix + ". Run downloadinstaller.");
    }

    private Path findForgeProfile(Path tmpDir) throws IOException {
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
        return forgeProfileFile;
    }

    void patchAuthlib(Path clientPath) throws IOException {
        Path pathToLauncherAuthlib = getPathToLauncherAuthlib();
        logger.info("Found launcher authlib in {}", pathToLauncherAuthlib);
        Path pathToOriginalAuthlib = findClientAuthlib(clientPath);
        logger.info("Found original authlib in {}", pathToOriginalAuthlib);
        Path temporary = Files.createTempFile(pathToOriginalAuthlib.getParent(), "authlib-", ".tmp");
        try {
            merge2Jars(pathToOriginalAuthlib, pathToLauncherAuthlib, temporary);
            Files.move(temporary, pathToOriginalAuthlib, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        logger.info("Authlib patched");
    }

    void checkAndDownloadVanilla(Path clientPath) throws Exception {
        Path fetchDir = cacheDir.resolve("clients").resolve("vanilla").resolve(version.toString());
        if (Files.notExists(fetchDir)) {
            Files.createDirectories(fetchDir.getParent());
            Path staging = Files.createTempDirectory(fetchDir.getParent(), "vanilla-");
            try {
                downloadVanillaTo(staging);
                Files.move(staging, fetchDir, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                if (Files.exists(staging)) {
                    IOHelper.deleteDir(staging, true);
                }
            }
        }
        copyDir(fetchDir, clientPath, path -> !(
                path.toString().contains("icu4j-core-mojang") &&
                        versionType == VersionType.FORGE &&
                        version.compareTo(ClientProfileVersions.MINECRAFT_1_12_2) == 0
        ));
    }

    Path getPathToLauncherAuthlib() {
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
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_1_21_9) < 0)  {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib6.jar");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_26_2) < 0)  {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib7.jar");
        } else if (version.compareTo(ClientProfileVersions.MINECRAFT_26_3) < 0) {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib9.jar");
        } else  {
            pathToLauncherAuthlib = workdir.resolve("authlib").resolve("LauncherAuthlib10.jar");
        }
        return pathToLauncherAuthlib;
    }

    void copyDir(Path source, Path target) throws IOException {
        copyDir(source, target, path -> true);
    }
    void copyDir(Path source, Path target, Predicate<Path> predicate) throws IOException {
        if (Files.notExists(source)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(source)) {
            for (var iterator = stream.iterator(); iterator.hasNext();) {
                Path from = iterator.next();
                Path relative = source.relativize(from);
                if (relative.toString().isEmpty() || !predicate.test(relative)) {
                    continue;
                }
                if (Files.isSymbolicLink(from)) {
                    throw new IOException("Unexpected symbolic link: " + from);
                }
                Path destination = WorkspaceManifest.resolve(target, relative.toString().replace(File.separatorChar, '/'));
                if (Files.isDirectory(from)) {
                    Files.createDirectories(destination);
                } else {
                    IOHelper.createParentDirs(destination);
                    IOHelper.copy(from, destination);
                }
            }
        }
    }

    Path findClientAuthlib(Path clientDir) throws IOException {
        try (Stream<Path> stream = Files.walk(clientDir).filter(p -> !Files.isDirectory(p) && p.getFileName().toString().startsWith("authlib-"))) {
            return stream.sorted().findFirst().orElseThrow();
        }
    }

    void merge2Jars(Path source, Path source2, Path target) throws IOException {
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

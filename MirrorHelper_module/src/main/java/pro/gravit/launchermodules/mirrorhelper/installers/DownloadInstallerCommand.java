package pro.gravit.launchermodules.mirrorhelper.installers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchermodules.mirrorhelper.InstallClient;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DownloadInstallerCommand extends Command {
    private final transient Logger logger = LogManager.getLogger(DownloadInstallerCommand.class);
    private final MirrorHelperModule module;
    public static final String FORGE_PROMOTIONS_URL = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    public static final String NEOFORGE_VERSIONS_URL = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge";
    public DownloadInstallerCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[FORGE/NEOFORGE] [MINECRAFT VERSION] (forge version or 'latest')";
    }

    @Override
    public String getUsageDescription() {
        return "download original forge/neoforge installer";
    }

    @Override
    public void invoke(String[] args) throws Exception {
        verifyArgs(args, 2);
        InstallClient.VersionType type = InstallClient.VersionType.valueOf(args[0]);
        ClientProfile.Version version = parseClientVersion(args[1]);
        String forgeVersion = args.length > 2 ? args[2] : null;
        switch (type) {
            case NEOFORGE -> {
                if(forgeVersion == null || forgeVersion.equals("latest")) {
                    NeoForgeVersionData neoForgeVersionData;
                    try(Reader reader = IOHelper.newReader(URI.create(NEOFORGE_VERSIONS_URL).toURL())) {
                        neoForgeVersionData = Launcher.gsonManager.gson.fromJson(reader, NeoForgeVersionData.class);
                    }
                    String key = version.toString();
                    boolean isSnapshot = !key.startsWith("1.");
                    String prefix;
                    if(isSnapshot) {
                        prefix = "0.".concat(key).concat(".");
                    } else {
                        prefix = key.substring(2).concat(".");
                    }
                    List<ClientProfile.Version> versions = new ArrayList<>();
                    for(var v : neoForgeVersionData.versions()) {
                        if(!v.startsWith(prefix)) {
                            continue;
                        }
                        versions.add(ClientProfile.Version.of(v));
                    }
                    if(versions.isEmpty()) {
                        throw new RuntimeException(String.format("Version '%s' not found (search prefix '%s')", version, prefix));
                    }
                    versions.sort(Comparator.naturalOrder());
                    ClientProfile.Version selectedVersion = versions.getLast();
                    logger.info("Found latest version {} for {}", selectedVersion.toString(), version.toString());
                    forgeVersion = selectedVersion.toString();
                }
                String url = "https://maven.neoforged.net/releases/net/neoforged/neoforge/${forge_version}/neoforge-${forge_version}-installer.jar"
                        .replace("${forge_version}", forgeVersion);
                Path path = module.getWorkspaceDir().resolve("installers").resolve(String.format("neoforge-%s-installer-nogui.jar", version));
                logger.info("Download {} into {}", url, path.toString());
                try(InputStream input = IOHelper.newInput(URI.create(url).toURL())) {
                    IOHelper.transfer(input, path);
                }
                logger.info("Download completed");
                Path cachePath = module.getWorkspaceDir().resolve("clients").resolve("neoforge").resolve(version.toString());
                if(Files.exists(cachePath)) {
                    logger.info("Clear cache {}", cachePath);
                    IOHelper.deleteDir(cachePath, true);
                    logger.info("Cache cleared");
                }
            }
            case FORGE -> {
                if(forgeVersion == null || forgeVersion.equals("latest")) {
                    ForgePromotionsData forgePromotionsData;
                    try(Reader reader = IOHelper.newReader(URI.create(FORGE_PROMOTIONS_URL).toURL())) {
                        forgePromotionsData = Launcher.gsonManager.gson.fromJson(reader, ForgePromotionsData.class);
                    }
                    String key = version.toString().concat("-latest");
                    forgeVersion = forgePromotionsData.promos().get(key);
                    if(forgeVersion == null) {
                        throw new IllegalArgumentException(String.format("Version '%s' not found", key));
                    }
                }
                String url;
                Path path;
                if(version.compareTo(ClientProfile.Version.of("1.10")) <= 0) {
                    url = "https://maven.minecraftforge.net/net/minecraftforge/forge/${version}-${forge_version}-${version}/forge-${version}-${forge_version}-${version}-installer.jar"
                            .replace("${version}", version.toString())
                            .replace("${forge_version}", forgeVersion);
                    path = module.getWorkspaceDir().resolve("installers").resolve(String.format("forge-%s-installer.jar", version));
                } else {
                    url = "https://maven.minecraftforge.net/net/minecraftforge/forge/${version}-${forge_version}/forge-${version}-${forge_version}-installer.jar"
                            .replace("${version}", version.toString())
                            .replace("${forge_version}", forgeVersion);
                    path = module.getWorkspaceDir().resolve("installers").resolve(String.format("forge-%s-installer-nogui.jar", version));
                }
                logger.info("Download {} into {}", url, path.toString());
                try(InputStream input = IOHelper.newInput(URI.create(url).toURL())) {
                    IOHelper.transfer(input, path);
                }
                logger.info("Download completed");
                Path cachePath = module.getWorkspaceDir().resolve("clients").resolve("forge").resolve(version.toString());
                if(Files.exists(cachePath)) {
                    logger.info("Clear cache {}", cachePath);
                    IOHelper.deleteDir(cachePath, true);
                    logger.info("Cache cleared");
                }
            }
            default -> {
                throw new UnsupportedOperationException(String.format("Unknown installer type %s", type));
            }
        }
    }

    public record ForgePromotionsData(String homepage, Map<String, String> promos) {

    }

    public record NeoForgeVersionData(boolean isSnapshot, List<String> versions) {

    }
}

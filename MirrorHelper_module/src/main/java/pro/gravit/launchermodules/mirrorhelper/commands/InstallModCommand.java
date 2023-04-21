package pro.gravit.launchermodules.mirrorhelper.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchermodules.mirrorhelper.modapi.CurseforgeAPI;
import pro.gravit.launchermodules.mirrorhelper.InstallClient;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.modapi.ModrinthAPI;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class InstallModCommand extends Command {

    private static final Logger logger = LogManager.getLogger();
    private final MirrorHelperModule module;

    public InstallModCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[dir] [version] [forge/fabric] [mod1,mod2,mod3]";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 4);
        Path dir = server.updatesDir.resolve(args[0]);
        if (Files.notExists(dir)) {
            throw new FileNotFoundException(dir.toString());
        }
        ClientProfile.Version version = parseClientVersion(args[1]);
        ModrinthAPI modrinthAPI = null;
        CurseforgeAPI curseforgeApi = null;
        Path modsDir = dir.resolve("mods");
        String loaderName = args[2];
        List<String> mods = Arrays.stream(args[3].split(",")).toList();
        if (!mods.isEmpty()) {
            for (var modId : mods) {
                try {
                    try {
                        long id = Long.parseLong(modId);
                        if (curseforgeApi == null) {
                            curseforgeApi = new CurseforgeAPI(module.config.curseforgeApiKey);
                        }
                        InstallClient.installMod(curseforgeApi, modsDir, id, version);
                        continue;
                    } catch (NumberFormatException ignored) {
                    }
                    if (modrinthAPI == null) {
                        modrinthAPI = new ModrinthAPI();
                    }
                    InstallClient.installMod(modrinthAPI, modsDir, modId, loaderName, version);
                } catch (Throwable e) {
                    logger.warn("Mod {} not installed! Exception {}", modId, e);
                }
            }
            logger.info("Mods installed");
        }
    }
}

package pro.gravit.launchermodules.mirrorhelper.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchermodules.mirrorhelper.CurseforgeAPI;
import pro.gravit.launchermodules.mirrorhelper.InstallClient;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class InstallModCommand extends Command {

    private static final transient Logger logger = LogManager.getLogger();
    private final MirrorHelperModule module;

    public InstallModCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[dir] [version] [mod1,mod2,mod3]";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        Path dir = server.updatesDir.resolve(args[0]);
        if (Files.notExists(dir)) {
            throw new FileNotFoundException(dir.toString());
        }
        ClientProfile.Version version = ClientProfile.Version.byName(args[1]);
        List<Long> mods = Arrays.stream(args[2].split(",")).map(Long::parseLong).toList();
        ;
        if (!mods.isEmpty()) {
            CurseforgeAPI api = new CurseforgeAPI(module.config.curseforgeApiKey);
            Path modsDir = dir.resolve("mods");
            for (var modId : mods) {
                InstallClient.installMod(api, modsDir, modId, version);
            }
            logger.info("Mods installed");
        }
    }
}

package pro.gravit.launchermodules.fileauthsystem.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthCoreProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.command.Command;

public class InstallCommand extends Command {
    private final Logger logger = LogManager.getLogger();

    public InstallCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "(authid)";
    }

    @Override
    public String getUsageDescription() {
        return "install fileauthsystem";
    }

    @Override
    public void invoke(String... args) throws Exception {
        AuthProviderPair pair;
        if (args.length > 0) {
            pair = server.config.getAuthProviderPair(args[0]);
        } else {
            pair = server.config.getAuthProviderPair();
        }
        if (pair == null) {
            throw new IllegalArgumentException("AuthProvider pair not found");
        }
        boolean changed = false;
        if (!(pair.core instanceof FileSystemAuthCoreProvider)) {
            if (pair.core != null) {
                pair.core.close();
                server.unregisterObject("auth.%s.core".formatted(pair.name), pair.core);
            }
            pair.core = new FileSystemAuthCoreProvider();
            pair.core.init(server);
            server.registerObject("auth.%s.core".formatted(pair.name), pair.core);
            logger.info("FileSystemAuthCoreProvider installed");
            changed = true;
        }
        if (changed) {
            server.launchServerConfigManager.writeConfig(server.config);
            logger.info("LaunchServer config updated");
        } else {
            logger.info("Already installed. Good!");
        }
    }
}

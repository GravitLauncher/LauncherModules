package pro.gravit.launchermodules.fileauthsystem.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthCoreProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.RejectAuthCoreProvider;
import pro.gravit.launchserver.auth.texture.VoidTextureProvider;
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

        if (!(pair.core instanceof RejectAuthCoreProvider)) {
            logger.warn("AuthProviderPair '{}' already has a non-reject core provider ({})",
                    pair.name, pair.core.getClass().getSimpleName());
            logger.warn("To avoid overwriting, a new auth provider 'fileauthsystem' will be created instead");

            if (server.config.auth.containsKey("fileauthsystem")) {
                logger.error("Auth provider 'fileauthsystem' already exists. Aborting.");
                return;
            }

            FileSystemAuthCoreProvider newCore = new FileSystemAuthCoreProvider();
            AuthProviderPair newPair = new AuthProviderPair(newCore, new VoidTextureProvider());
            newPair.displayName = "FileAuthSystem";
            newPair.isDefault = false;
            newCore.init(server, newPair);
            server.config.auth.put("fileauthsystem", newPair);
            newPair.init(server, "fileauthsystem");
            server.registerObject("auth.fileauthsystem.core", newCore);
            server.launchServerConfigManager.writeConfig(server.config);
            logger.info("New auth provider 'fileauthsystem' created with FileSystemAuthCoreProvider");
            return;
        }

        pair.core.close();
        server.unregisterObject("auth.%s.core".formatted(pair.name), pair.core);
        pair.core = new FileSystemAuthCoreProvider();
        pair.core.init(server, pair);
        server.registerObject("auth.%s.core".formatted(pair.name), pair.core);
        server.launchServerConfigManager.writeConfig(server.config);
        logger.info("FileSystemAuthCoreProvider installed to '{}'", pair.name);
    }
}

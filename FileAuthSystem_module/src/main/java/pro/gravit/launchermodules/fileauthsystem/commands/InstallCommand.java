package pro.gravit.launchermodules.fileauthsystem.commands;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthHandler;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class InstallCommand extends Command {
    private final FileAuthSystemModule module;
    public InstallCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.module = module;
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
        if(args.length > 0) {
            pair = server.config.getAuthProviderPair(args[0]);
        } else {
            pair = server.config.getAuthProviderPair();
        }
        if(pair == null) {
            throw new IllegalArgumentException("AuthProvider pair not found");
        }
        boolean changed = false;
        if(!(pair.provider instanceof FileSystemAuthProvider)) {
            pair.provider.close();
            pair.provider = new FileSystemAuthProvider();
            pair.provider.init(server);
            changed = true;
            LogHelper.info("AuthProvider successful changed");
        }
        if(!(pair.handler instanceof FileSystemAuthHandler)) {
            pair.handler.close();
            pair.handler = new FileSystemAuthHandler();
            pair.handler.init(server);
            changed = true;
            LogHelper.info("AuthHandler successful changed");
        }
        if(changed) {
            server.launchServerConfigManager.writeConfig(server.config);
            LogHelper.info("LaunchServer config updated");
        } else {
            LogHelper.info("Already installed. Good!");
        }
    }
}

package pro.gravit.launchermodules.fileauthsystem;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthCoreProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

import java.nio.file.Path;

public class FileAuthSystemModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    public JsonConfigurable<FileAuthSystemConfig> jsonConfigurable;

    public FileAuthSystemModule() {
        super(new LauncherModuleInfoBuilder().setName("FileAuthSystem").setVersion(version).setDependencies(new String[]{"LaunchServerCore"}).createLauncherModuleInfo());
    }

    public void preConfig(PreConfigPhase preConfigPhase) {
        registerProviders();
    }

    public void finish(LaunchServerPostInitPhase event) {
        registerCommands(event.server);
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if(initContext instanceof LaunchServerInitContext context) {
            registerProviders();
            registerCommands(context.server);
            Launcher.gsonManager.initGson(); // Hot reload gson parser
        }
        registerEvent(this::preConfig, PreConfigPhase.class);
        registerEvent(this::finish, LaunchServerPostInitPhase.class);
        Path dbPath = modulesConfigManager.getModuleConfigDir(moduleInfo.name);
        jsonConfigurable = modulesConfigManager.getConfigurable(FileAuthSystemConfig.class, moduleInfo.name);
    }

    private void registerProviders() {
        AuthCoreProvider.providers.register("fileauthsystem", FileSystemAuthCoreProvider.class);
    }

    private void registerCommands(LaunchServer server) {
        server.commandHandler.registerCommand("fileauthsystem", new FileAuthSystemCommand(server, this));
    }

    public Path getDatabasePath() {
        return modulesConfigManager.getModuleConfigDir(moduleInfo.name);
    }

}

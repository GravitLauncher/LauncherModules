package pro.gravit.launchermodules.fileauthsystem;

import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthCoreProvider;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.utils.Version;

import java.nio.file.Path;

public class FileAuthSystemModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    public JsonConfigurable<FileAuthSystemConfig> jsonConfigurable;

    public FileAuthSystemModule() {
        super(new LauncherModuleInfo("FileAuthSystem", version, new String[]{"LaunchServerCore"}));
    }

    public void preConfig(PreConfigPhase preConfigPhase) {
        AuthCoreProvider.providers.register("fileauthsystem", FileSystemAuthCoreProvider.class);
    }

    public void finish(LaunchServerPostInitPhase event) {
        event.server.commandHandler.registerCommand("fileauthsystem", new FileAuthSystemCommand(event.server, this));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preConfig, PreConfigPhase.class);
        registerEvent(this::finish, LaunchServerPostInitPhase.class);
        Path dbPath = modulesConfigManager.getModuleConfigDir(moduleInfo.name);
        jsonConfigurable = modulesConfigManager.getConfigurable(FileAuthSystemConfig.class, moduleInfo.name);
    }

    public Path getDatabasePath() {
        return modulesConfigManager.getModuleConfigDir(moduleInfo.name);
    }

}

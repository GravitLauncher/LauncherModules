package pro.gravit.launchermodules.fileauthsystem;

import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthCoreProvider;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.SecurityHelper;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class FileAuthSystemModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    public JsonConfigurable<FileAuthSystemConfig> jsonConfigurable;
    private Path dbPath;

    public FileAuthSystemModule() {
        super(new LauncherModuleInfo("FileAuthSystem", version, new String[]{"LaunchServerCore"}));
    }

    public void preConfig(PreConfigPhase preConfigPhase) {
        AuthCoreProvider.providers.register("fileauthsystem", FileSystemAuthCoreProvider.class);
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preConfig, PreConfigPhase.class);
        dbPath = modulesConfigManager.getModuleConfigDir(moduleInfo.name);
        jsonConfigurable = modulesConfigManager.getConfigurable(FileAuthSystemConfig.class, moduleInfo.name);
    }

    public Path getDatabasePath() {
        return modulesConfigManager.getModuleConfigDir(moduleInfo.name);
    }

    public static class UserSessionEntity implements UserSession {
        private final UUID uuid;
        public transient FileSystemAuthCoreProvider.UserEntity entity;
        public UUID userEntityUUID;
        public String accessToken;
        public String refreshToken;
        public long expireMillis;

        public UserSessionEntity(FileSystemAuthCoreProvider.UserEntity entity) {
            this.uuid = UUID.randomUUID();
            this.entity = entity;
            this.accessToken = SecurityHelper.randomStringToken();
            this.refreshToken = SecurityHelper.randomStringToken();
            this.expireMillis = 0;
            this.userEntityUUID = entity.uuid;
        }

        public void update(long expireMillis) {
            this.expireMillis = System.currentTimeMillis() + expireMillis;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserSessionEntity entity = (UserSessionEntity) o;
            return Objects.equals(uuid, entity.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }

        @Override
        public String getID() {
            return uuid.toString();
        }

        @Override
        public User getUser() {
            return entity;
        }

        @Override
        public long getExpireIn() {
            return expireMillis;
        }
    }
}

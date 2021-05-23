package pro.gravit.launchermodules.fileauthsystem;

import com.google.gson.reflect.TypeToken;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.ClosePhase;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthCoreProvider;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthHandler;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileAuthSystemModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    public JsonConfigurable<FileAuthSystemConfig> jsonConfigurable;
    public Map<UUID, UserEntity> users = new ConcurrentHashMap<>();
    public Set<UserSessionEntity> sessions = ConcurrentHashMap.newKeySet();
    private Path dbPath;

    public FileAuthSystemModule() {
        super(new LauncherModuleInfo("FileAuthSystem", version, new String[]{"LaunchServerCore"}));
    }

    public UserEntity getUser(String username) {
        for (UserEntity e : users.values()) {
            if (username.equals(e.username)) {
                return e;
            }
        }
        return null;
    }

    public UserSessionEntity getSessionByAccessToken(String accessToken) {
        return sessions.stream().filter(e -> e.accessToken != null && e.accessToken.equals(accessToken)).findFirst().orElse(null);
    }

    public UserSessionEntity getSessionByRefreshToken(String refreshToken) {
        return sessions.stream().filter(e -> e.accessToken != null && e.refreshToken.equals(refreshToken)).findFirst().orElse(null);
    }

    public void addNewSession(UserSessionEntity session) {
        sessions.add(session);
    }

    public UserEntity getUser(UUID uuid) {
        return users.get(uuid);
    }

    public Collection<UserEntity> getAllUsers() {
        return users.values();
    }

    public void addUser(UserEntity entity) {
        users.put(entity.uuid, entity);
    }

    public void deleteUser(UserEntity entity) {
        users.remove(entity.uuid);
    }

    public void deleteUser(UUID uuid) {
        users.remove(uuid);
    }

    public void finish(LaunchServerFullInitEvent event) {
        LaunchServer launchServer = event.server;
        try {
            jsonConfigurable.loadConfig();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        launchServer.commandHandler.registerCommand("fileauthsystem", new FileAuthSystemCommand(launchServer, this));
        load();
    }

    public void exit(ClosePhase closePhase) {
        if (jsonConfigurable != null && jsonConfigurable.getConfig() != null && jsonConfigurable.getConfig().autoSave)
            save();
    }

    public void preConfig(PreConfigPhase preConfigPhase) {
        AuthProvider.providers.register("fileauthsystem", FileSystemAuthProvider.class);
        AuthHandler.providers.register("fileauthsystem", FileSystemAuthHandler.class);
        AuthCoreProvider.providers.register("fileauthsystem", FileSystemAuthCoreProvider.class);
    }

    public void load() {
        load(dbPath);
    }

    public void load(Path path) {
        if (!Files.exists(path)) return;
        Type collectionType = new TypeToken<Map<UUID, UserEntity>>() {
        }.getType();
        try (Reader reader = IOHelper.newReader(path)) {
            this.users = Launcher.gsonManager.configGson.fromJson(reader, collectionType);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public void save() {
        save(dbPath);
    }

    public void save(Path path) {
        Type collectionType = new TypeToken<Map<UUID, UserEntity>>() {
        }.getType();
        try (Writer writer = IOHelper.newWriter(path)) {
            Launcher.gsonManager.configGson.toJson(users, collectionType, writer);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
        registerEvent(this::preConfig, PreConfigPhase.class);
        registerEvent(this::exit, ClosePhase.class);
        dbPath = modulesConfigManager.getModuleConfig(moduleInfo.name, "Database");
        jsonConfigurable = modulesConfigManager.getConfigurable(FileAuthSystemConfig.class, moduleInfo.name);
    }

    public static class UserEntity implements User {
        public String username;
        public UUID uuid;
        public ClientPermissions permissions;
        public String serverId;
        public String accessToken;
        private byte[] password;

        public UserEntity() {
            permissions = new ClientPermissions();
        }

        public UserEntity(String username) {
            this.username = username;
            this.uuid = UUID.randomUUID();
            this.permissions = new ClientPermissions();
        }

        public UserEntity(String username, String password) {
            this.username = username;
            this.uuid = UUID.randomUUID();
            this.permissions = new ClientPermissions();
            this.setPassword(password);
        }

        public void setPassword(String password) {
            this.password = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, password);
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public UUID getUUID() {
            return uuid;
        }

        @Override
        public String getServerId() {
            return serverId;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public ClientPermissions getPermissions() {
            return permissions;
        }

        public boolean verifyPassword(String password) {
            return Arrays.equals(this.password, SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, password));
        }
    }

    public static class UserSessionEntity implements UserSession {
        private final UUID uuid;
        public UserEntity entity;
        public String accessToken;
        public String refreshToken;
        public long expireMillis;

        public UserSessionEntity(UserEntity entity) {
            this.uuid = UUID.randomUUID();
            this.entity = entity;
            this.accessToken = SecurityHelper.randomStringToken();
            this.refreshToken = SecurityHelper.randomStringToken();
            this.expireMillis = 0;
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

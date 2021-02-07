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
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthHandler;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemAuthProvider;
import pro.gravit.launchermodules.fileauthsystem.providers.FileSystemDAOProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.provider.DaoProvider;
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
    private Path dbPath;
    public JsonConfigurable<FileAuthSystemConfig> jsonConfigurable;
    public static class UserEntity implements User {
        public String username;
        private byte[] password;
        public UUID uuid;
        public ClientPermissions permissions;
        public String serverId;
        public String accessToken;

        public void setPassword(String password) {
            this.password = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, password);
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public String getServerID() {
            return serverId;
        }

        @Override
        public void setServerID(String serverID) {
            this.serverId = serverID;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public ClientPermissions getPermissions() {
            return permissions;
        }

        @Override
        public void setPermissions(ClientPermissions permissions) {
            this.permissions = permissions;
        }

        public boolean verifyPassword(String password) {
            return Arrays.equals( this.password,  SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA256, password));
        }

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
    }
    public Map<UUID, UserEntity> users = new ConcurrentHashMap<>();

    public UserEntity getUser(String username) {
        for(UserEntity e : users.values()) {
            if(username.equals(e.username)) {
                return e;
            }
        }
        return null;
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

    public FileAuthSystemModule() {
        super(new LauncherModuleInfo("FileAuthSystem", version, new String[]{"LaunchServerCore"}));
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
        if(jsonConfigurable != null && jsonConfigurable.getConfig() != null && jsonConfigurable.getConfig().autoSave)
            save();
    }

    public void preConfig(PreConfigPhase preConfigPhase) {
        AuthProvider.providers.register("fileauthsystem", FileSystemAuthProvider.class);
        AuthHandler.providers.register("fileauthsystem", FileSystemAuthHandler.class);
        DaoProvider.providers.register("fileauthsystem", FileSystemDAOProvider.class);
    }

    public void load() {
        load(dbPath);
    }

    public void load(Path path) {
        if(!Files.exists(path)) return;
        Type collectionType = new TypeToken<Map<UUID, UserEntity>>() {}.getType();
        try(Reader reader = IOHelper.newReader(path)) {
            this.users = Launcher.gsonManager.configGson.fromJson(reader, collectionType);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public void save() {
        save(dbPath);
    }

    public void save(Path path) {
        Type collectionType = new TypeToken<Map<UUID, UserEntity>>() {}.getType();
        try(Writer writer = IOHelper.newWriter(path)) {
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
}

package pro.gravit.launchermodules.fileauthsystem.providers;

import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemConfig;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportExit;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportRegistration;
import pro.gravit.launchserver.auth.password.DigestPasswordVerifier;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileSystemAuthCoreProvider extends AuthCoreProvider implements AuthSupportRegistration, AuthSupportExit {
    public String databaseDir;
    public boolean autoSave = true;
    public boolean autoReg = false;
    public long oauthTokenExpire = 60 * 60 * 1000;
    public PasswordVerifier passwordVerifier;
    private final transient Logger logger = LogManager.getLogger();
    private transient Map<UUID, UserEntity> users = new ConcurrentHashMap<>();
    private transient Set<FileAuthSystemModule.UserSessionEntity> sessions = ConcurrentHashMap.newKeySet();
    private transient FileAuthSystemModule module;
    private transient Path dbPath;

    @Override
    public Map<String, Command> getCommands() {
        var commands = super.getCommands();
        commands.put("changePassword", new SubCommand("[username] [new password]", "Change user password") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                UserEntity entity = getUser(args[0]);
                if (entity == null)
                    throw new IllegalArgumentException(String.format("User %s not found", args[0]));
                entity.setPassword(passwordVerifier.encrypt(args[1]));
                logger.info("Password changed");
            }
        });
        commands.put("xconvertpassword", new SubCommand("[]", "Convert 5.2.2 and lower base64 sha256 passwords") {
            @Override
            public void invoke(String... args) throws Exception {
                for (var e : users.entrySet()) {
                    byte[] rawPassword = Base64.getUrlDecoder().decode(e.getValue().password);
                    e.getValue().setPassword(SecurityHelper.toHex(rawPassword));
                }
            }
        });
        commands.put("reload", new SubCommand("[]", "Reload database") {
            @Override
            public void invoke(String... args) throws Exception {
                load();
            }
        });
        commands.put("save", new SubCommand("[]", "Save database") {
            @Override
            public void invoke(String... args) throws Exception {
                save();
            }
        });
        commands.put("addpermission", new SubCommand("[username] [permission]", "add user permission") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);
                UserEntity entity = getUser(args[0]);
                if (entity == null)
                    throw new IllegalArgumentException(String.format("User %s not found", args[0]));
                entity.getPermissions().addPerm(args[1]);
                logger.info("Permission added");
            }
        });
        commands.put("removepermission", new SubCommand("[username] [permission]", "remove user permission") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);
                UserEntity entity = getUser(args[0]);
                if (entity == null)
                    throw new IllegalArgumentException(String.format("User %s not found", args[0]));
                entity.getPermissions().removePerm(args[1]);
                logger.info("Permission added");
            }
        });
        return commands;
    }

    @Override
    public User getUserByUsername(String username) {
        return getUser(username);
    }

    @Override
    public User getUserByLogin(String login) {
        if (autoReg) {
            User user = getUser(login);
            if (user == null) {
                user = registration(login, null, new AuthPlainPassword(""), null);
            }
            return user;
        }
        return getUserByUsername(login);
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        return getUser(uuid);
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        FileAuthSystemModule.UserSessionEntity session = getSessionByAccessToken(accessToken);
        if (session == null) return null;
        if (session.expireMillis != 0 && session.expireMillis < System.currentTimeMillis())
            throw new OAuthAccessTokenExpired();
        return session;
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        FileAuthSystemConfig config = module.jsonConfigurable.getConfig();
        FileAuthSystemModule.UserSessionEntity session = getSessionByRefreshToken(refreshToken);
        if (session == null) return null;
        session.refreshToken = SecurityHelper.randomStringToken();
        session.accessToken = SecurityHelper.randomStringToken();
        if (oauthTokenExpire != 0) {
            session.update(oauthTokenExpire);
        }
        return AuthManager.AuthReport.ofOAuth(session.accessToken, session.refreshToken, oauthTokenExpire);
    }

    @Override
    public void verifyAuth(AuthResponse.AuthContext context) throws AuthException {
        // None
    }

    @Override
    public PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password) {
        UserEntity entity = (UserEntity) user;
        if (!(password instanceof AuthPlainPassword plainPassword)) {
            return PasswordVerifyReport.FAILED;
        }
        if (passwordVerifier.check(entity.password, plainPassword.password)) {
            return PasswordVerifyReport.OK;
        }
        return PasswordVerifyReport.FAILED;
    }

    @Override
    public AuthManager.AuthReport createOAuthSession(User user, AuthResponse.AuthContext context, PasswordVerifyReport report, boolean minecraftAccess) throws IOException {
        FileAuthSystemConfig config = module.jsonConfigurable.getConfig();
        FileAuthSystemModule.UserSessionEntity entity = new FileAuthSystemModule.UserSessionEntity((UserEntity) user);
        addNewSession(entity);
        if (oauthTokenExpire != 0) {
            entity.update(oauthTokenExpire);
        }
        if (minecraftAccess) {
            String minecraftAccessToken = SecurityHelper.randomStringToken();
            ((UserEntity) user).accessToken = minecraftAccessToken;
            return AuthManager.AuthReport.ofOAuthWithMinecraft(minecraftAccessToken, entity.accessToken, entity.refreshToken, oauthTokenExpire);
        }
        return AuthManager.AuthReport.ofOAuth(entity.accessToken, entity.refreshToken, oauthTokenExpire);
    }

    @Override
    public void init(LaunchServer server) {
        module = server.modulesManager.getModule(FileAuthSystemModule.class);
        dbPath = module.getDatabasePath();
        if (passwordVerifier == null) {
            var verifier = new DigestPasswordVerifier();
            verifier.algo = "SHA256";
            passwordVerifier = verifier;
        }
        if (databaseDir != null) {
            dbPath = Paths.get(databaseDir);
            try {
                IOHelper.createParentDirs(dbPath);
            } catch (IOException e) {
                logger.error("CreateParentDirs failed", e);
                return;
            }
        }
        load();
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        UserEntity entity = (UserEntity) user;
        if (entity == null) return false;
        entity.serverId = serverID;
        return true;
    }

    @Override
    public void close() throws IOException {
        if (autoSave) {
            save();
        }
    }

    @Override
    public User registration(String login, String email, AuthRequest.AuthPasswordInterface password, Map<String, String> properties) {
        String hashedPassword = passwordVerifier.encrypt(((AuthPlainPassword) password).password);
        UserEntity entity = new UserEntity(login, hashedPassword);
        try {
            addUser(entity);
        } catch (RequestException e) {
            return null;
        }
        return entity;
    }

    @Override
    public boolean deleteSession(UserSession session) {
        return deleteSession((FileAuthSystemModule.UserSessionEntity) session);
    }

    @Override
    public boolean exitUser(User user) {
        return exitUser((UserEntity) user);
    }


    public void load() {
        load(dbPath);
    }

    public void load(Path path) {
        {
            Path databasePath = path.resolve("Database.json");
            if (!Files.exists(databasePath)) return;
            Type databaseType = new TypeToken<Map<UUID, UserEntity>>() {
            }.getType();
            try (Reader reader = IOHelper.newReader(databasePath)) {
                this.users = Launcher.gsonManager.configGson.fromJson(reader, databaseType);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        {
            Path sessionsPath = path.resolve("Sessions.json");
            if (!Files.exists(sessionsPath)) return;
            Type sessionsType = new TypeToken<Set<FileAuthSystemModule.UserSessionEntity>>() {
            }.getType();
            try (Reader reader = IOHelper.newReader(sessionsPath)) {
                this.sessions = Launcher.gsonManager.configGson.fromJson(reader, sessionsType);
            } catch (IOException e) {
                LogHelper.error(e);
            }
            for (FileAuthSystemModule.UserSessionEntity sessionEntity : sessions) {
                if (sessionEntity.userEntityUUID != null) {
                    sessionEntity.entity = getUser(sessionEntity.userEntityUUID);
                }
            }
        }
    }

    public void save() {
        save(dbPath);
    }

    public void save(Path path) {

        {
            Path databasePath = path.resolve("Database.json");
            Type databaseType = new TypeToken<Map<UUID, UserEntity>>() {
            }.getType();
            try (Writer writer = IOHelper.newWriter(databasePath)) {
                Launcher.gsonManager.configGson.toJson(users, databaseType, writer);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        {
            Path sessionsPath = path.resolve("Sessions.json");
            Type sessionsType = new TypeToken<Set<FileAuthSystemModule.UserSessionEntity>>() {
            }.getType();
            try (Writer writer = IOHelper.newWriter(sessionsPath)) {
                Launcher.gsonManager.configGson.toJson(sessions, sessionsType, writer);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
    }

    private UserEntity getUser(String username) {
        for (UserEntity e : users.values()) {
            if (username.equals(e.username)) {
                return e;
            }
        }
        return null;
    }

    private FileAuthSystemModule.UserSessionEntity getSessionByAccessToken(String accessToken) {
        return sessions.stream().filter(e -> e.accessToken != null && e.accessToken.equals(accessToken)).findFirst().orElse(null);
    }

    private FileAuthSystemModule.UserSessionEntity getSessionByRefreshToken(String refreshToken) {
        return sessions.stream().filter(e -> e.accessToken != null && e.refreshToken.equals(refreshToken)).findFirst().orElse(null);
    }

    private void addNewSession(FileAuthSystemModule.UserSessionEntity session) {
        sessions.add(session);
    }

    private UserEntity getUser(UUID uuid) {
        return users.get(uuid);
    }

    private Collection<UserEntity> getAllUsers() {
        return users.values();
    }

    private void addUser(UserEntity entity) throws RequestException {
        if (getUser(entity.username) != null) {
            throw new RequestException("User already registered");
        }
        users.put(entity.uuid, entity);
    }

    private void deleteUser(UserEntity entity) {
        users.remove(entity.uuid);
    }

    private void deleteUser(UUID uuid) {
        users.remove(uuid);
    }

    private boolean deleteSession(FileAuthSystemModule.UserSessionEntity entity) {
        return sessions.remove(entity);
    }

    private boolean exitUser(UserEntity user) {
        return sessions.removeIf(e -> e.entity == user);
    }

    public static class UserEntity implements User {
        public String username;
        public UUID uuid;
        public ClientPermissions permissions;
        public String serverId;
        public String accessToken;
        private String password;

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
            this.password = password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        public void setPassword(String password) {
            this.password = password;
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

        @Override
        public String toString() {
            return "UserEntity{" +
                    "username='" + username + '\'' +
                    ", uuid=" + uuid +
                    ", permissions=" + permissions +
                    '}';
        }
    }
}

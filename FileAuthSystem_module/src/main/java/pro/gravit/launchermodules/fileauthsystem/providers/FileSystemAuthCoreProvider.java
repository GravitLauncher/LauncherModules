package pro.gravit.launchermodules.fileauthsystem.providers;

import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.ClientPermissions;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.Texture;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.password.AuthPlainPassword;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemConfig;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportExit;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportRegistration;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportSudo;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportTextures;
import pro.gravit.launchserver.auth.password.DigestPasswordVerifier;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.auth.texture.RequestTextureProvider;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.CommonHelper;
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

import static java.util.concurrent.TimeUnit.HOURS;

public class FileSystemAuthCoreProvider extends AuthCoreProvider implements AuthSupportRegistration, AuthSupportExit, AuthSupportSudo {
    private final transient Logger logger = LogManager.getLogger();
    public String databaseDir;
    public boolean autoSave = true;
    public boolean autoReg = false;
    public long oauthTokenExpire = HOURS.toMillis(1);
    public PasswordVerifier passwordVerifier;
    public String skinUrl;
    public String cloakUrl;
    private transient Map<UUID, UserEntity> users = new ConcurrentHashMap<>();
    private transient Set<UserSessionEntity> sessions = ConcurrentHashMap.newKeySet();
    private transient FileAuthSystemModule module;
    private transient Path dbPath;
    private transient LaunchServer server;

    @Override
    public Map<String, Command> getCommands() {
        var commands = super.getCommands();
        commands.put("changePassword", new SubCommand("[username] [new password]", "Change user password") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                UserEntity entity = getUser(args[0]);
                if (entity == null)
                    throw new IllegalArgumentException("User %s not found".formatted(args[0]));
                entity.setPassword(passwordVerifier.encrypt(args[1]));
                exitUser(entity);
                logger.info("Password changed");
            }
        });
        commands.put("xconvertpassword", new SubCommand("[]", "Convert 5.2.2 and lower base64 sha256 passwords") {
            @Override
            public void invoke(String... args) {
                for (var e : users.entrySet()) {
                    byte[] rawPassword = Base64.getUrlDecoder().decode(e.getValue().password);
                    e.getValue().setPassword(SecurityHelper.toHex(rawPassword));
                }
            }
        });
        commands.put("reload", new SubCommand("[]", "Reload database") {
            @Override
            public void invoke(String... args) {
                load();
            }
        });
        commands.put("save", new SubCommand("[]", "Save database") {
            @Override
            public void invoke(String... args) {
                save();
            }
        });
        commands.put("addpermission", new SubCommand("[username] [permission]", "add user permission") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);
                UserEntity entity = getUser(args[0]);
                if (entity == null)
                    throw new IllegalArgumentException("User %s not found".formatted(args[0]));
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
                    throw new IllegalArgumentException("User %s not found".formatted(args[0]));
                entity.getPermissions().removePerm(args[1]);
                logger.info("Permission added");
            }
        });
        commands.put("updateskin", new SubCommand("[username] [slim (true/false)] (url or path)", "update skin for user") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);
                UserEntity entity = getUser(args[0]);
                if (entity == null)
                    throw new IllegalArgumentException("User %s not found".formatted(args[0]));
                boolean isSlim = Boolean.parseBoolean(args[1]);
                Map<String, String> metadata = isSlim ? Map.of("model", "slim") : null;
                Texture texture;
                if (args.length >= 3) {
                    String textureUrl = args[2];
                    if (!textureUrl.startsWith("http://") && !textureUrl.startsWith("https://")) {
                        Path pathToSkin = Paths.get(textureUrl);
                        byte[] digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, pathToSkin);
                        String hexDigest = SecurityHelper.toHex(digest);
                        Path target = server.updatesDir.resolve("skins").resolve(hexDigest);
                        IOHelper.createParentDirs(target);
                        if (Files.notExists(target)) {
                            Files.copy(pathToSkin, target);
                        }
                        String url = CommonHelper.replace(server.config.netty.downloadURL, "dirname", "skins").concat(hexDigest);
                        texture = new Texture(url, digest, metadata);
                    } else {
                        texture = new Texture(textureUrl, false, metadata);
                    }
                } else if (skinUrl != null) {
                    String textureUrl = RequestTextureProvider.getTextureURL(skinUrl, entity.uuid, entity.username, "");
                    texture = new Texture(textureUrl, false, metadata);
                } else {
                    throw new IllegalArgumentException("Please provide url or path");
                }
                entity.skin = texture;
            }
        });
        commands.put("updatecloak", new SubCommand("[username] (url or path)", "update cloak for user") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                UserEntity entity = getUser(args[0]);
                if (entity == null)
                    throw new IllegalArgumentException("User %s not found".formatted(args[0]));
                Texture texture;
                if (args.length >= 2) {
                    String textureUrl = args[1];
                    if (!textureUrl.startsWith("http://") && !textureUrl.startsWith("https://")) {
                        Path pathToSkin = Paths.get(textureUrl);
                        byte[] digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, pathToSkin);
                        String hexDigest = SecurityHelper.toHex(digest);
                        Path target = server.updatesDir.resolve("skins").resolve(hexDigest);
                        IOHelper.createParentDirs(target);
                        if (Files.notExists(target)) {
                            Files.copy(pathToSkin, target);
                        }
                        String url = CommonHelper.replace(server.config.netty.downloadURL, "dirname", "skins").concat(hexDigest);
                        texture = new Texture(url, digest, null);
                    } else {
                        texture = new Texture(textureUrl, true, null);
                    }
                } else if (cloakUrl != null) {
                    String textureUrl = RequestTextureProvider.getTextureURL(cloakUrl, entity.uuid, entity.username, "");
                    texture = new Texture(textureUrl, true, null);
                } else {
                    throw new IllegalArgumentException("Please provide url or path");
                }
                entity.cloak = texture;
            }
        });
        return commands;
    }

    @Override
    public User checkServer(Client client, String username, String serverID) {
        UserSessionEntity session = getSessionByServerId(serverID);
        if(session == null) {
            return null;
        }
        if(session.entity.username.equals(username)) {
            return session.entity;
        }
        return null;
    }

    @Override
    public boolean joinServer(Client client, String username, UUID uuid, String accessToken, String serverID) {
        UserSessionEntity session = getSessionByMinecraftAccessToken(accessToken);
        if(session == null) {
            return false;
        }
        if(uuid != null) {
            if(session.entity.uuid.equals(uuid)) {
                session.serverId = serverID;
                return true;
            }
        } else {
            if(session.entity.username.equals(username)) {
                session.serverId = serverID;
                return true;
            }
        }
        return false;
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
        UserSessionEntity session = getSessionByAccessToken(accessToken);
        if (session == null) return null;
        if (session.expireMillis != 0 && session.expireMillis < System.currentTimeMillis())
            throw new OAuthAccessTokenExpired();
        return session;
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        FileAuthSystemConfig config = module.jsonConfigurable.getConfig();
        UserSessionEntity session = getSessionByRefreshToken(refreshToken);
        if (session == null) return null;
        session.refreshToken = SecurityHelper.randomStringToken();
        session.accessToken = SecurityHelper.randomStringToken();
        if (oauthTokenExpire != 0) {
            session.update(oauthTokenExpire);
        }
        return AuthManager.AuthReport.ofOAuth(session.accessToken, session.refreshToken, oauthTokenExpire, session);
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        UserEntity user = getUser(login);
        if (user == null) {
            throw AuthException.userNotFound();
        }
        if (context != null) {
            AuthPlainPassword plainPassword = (AuthPlainPassword) password;
            if (password == null) {
                throw AuthException.wrongPassword();
            }
            if (!passwordVerifier.check(user.password, plainPassword.password)) {
                throw AuthException.wrongPassword();
            }
        }
        UserSessionEntity session = new UserSessionEntity(user);
        addNewSession(session);
        if (oauthTokenExpire != 0) {
            session.update(oauthTokenExpire);
        }
        if (minecraftAccess) {
            return AuthManager.AuthReport.ofOAuthWithMinecraft(session.minecraftAccessToken, session.accessToken, session.refreshToken, oauthTokenExpire, session);
        }
        return AuthManager.AuthReport.ofOAuth(session.accessToken, session.refreshToken, oauthTokenExpire, session);
    }

    @Override
    public AuthManager.AuthReport sudo(User user, boolean shadow) {
        UserEntity entity = (UserEntity) user;
        UserSessionEntity session = new UserSessionEntity(entity);
        addNewSession(session);
        return AuthManager.AuthReport.ofOAuthWithMinecraft(session.minecraftAccessToken, session.accessToken, session.refreshToken, oauthTokenExpire, session);
    }

    @Override
    public void init(LaunchServer server, AuthProviderPair pair) {
        this.server = server;
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
    public void close() {
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
            throw new RuntimeException("Failed to add user", e);
        }
        return entity;
    }

    @Override
    public void deleteSession(UserSession session) {
        deleteSession((UserSessionEntity) session);
    }

    @Override
    public void exitUser(User user) {
        exitUser((UserEntity) user);
    }


    public void load() {
        load(dbPath);
    }

    public void load(Path path) {
        boolean saveRequired = false;
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
            Type sessionsType = new TypeToken<Set<UserSessionEntity>>() {
            }.getType();
            try (Reader reader = IOHelper.newReader(sessionsPath)) {
                this.sessions = Launcher.gsonManager.configGson.fromJson(reader, sessionsType);
            } catch (IOException e) {
                LogHelper.error(e);
            }
            for (UserSessionEntity sessionEntity : sessions) {
                if (sessionEntity.userEntityUUID != null) {
                    sessionEntity.entity = getUser(sessionEntity.userEntityUUID);
                }
                if(sessionEntity.minecraftAccessToken == null) {
                    sessionEntity.minecraftAccessToken = SecurityHelper.randomStringToken();
                    saveRequired = true;
                }
            }
        }
        if(saveRequired) {
            logger.warn("FileAuthSystem: converted old database {}", path.toAbsolutePath().toString());
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
            Type sessionsType = new TypeToken<Set<UserSessionEntity>>() {
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

    private UserSessionEntity getSessionByAccessToken(String accessToken) {
        return sessions.stream().filter(e -> e.accessToken != null && e.accessToken.equals(accessToken)).findFirst().orElse(null);
    }

    private UserSessionEntity getSessionByMinecraftAccessToken(String minecraftAccessToken) {
        return sessions.stream().filter(e -> e.minecraftAccessToken != null && e.minecraftAccessToken.equals(minecraftAccessToken)).findFirst().orElse(null);
    }

    private UserSessionEntity getSessionByServerId(String serverId) {
        return sessions.stream().filter(e -> e.serverId != null && e.serverId.equals(serverId)).findFirst().orElse(null);
    }

    private UserSessionEntity getSessionByRefreshToken(String refreshToken) {
        return sessions.stream().filter(e -> e.accessToken != null && e.refreshToken.equals(refreshToken)).findFirst().orElse(null);
    }

    private void addNewSession(UserSessionEntity session) {
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

    private boolean deleteSession(UserSessionEntity entity) {
        return sessions.remove(entity);
    }

    private boolean exitUser(UserEntity user) {
        return sessions.removeIf(e -> e.entity == user);
    }

    public static class UserEntity implements User, UserSupportTextures {
        public String username;
        public UUID uuid;
        public ClientPermissions permissions;
        public Texture skin;
        public Texture cloak;
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

        @Override
        public Texture getSkinTexture() {
            return skin;
        }

        @Override
        public Texture getCloakTexture() {
            return cloak;
        }
    }

    public static class UserSessionEntity implements UserSession {
        private final UUID uuid;
        public transient UserEntity entity;
        public UUID userEntityUUID;
        public String accessToken;
        public String refreshToken;
        public String serverId;
        public String minecraftAccessToken;
        public long expireMillis;

        public UserSessionEntity(UserEntity entity) {
            this.uuid = UUID.randomUUID();
            this.entity = entity;
            this.accessToken = SecurityHelper.randomStringToken();
            this.refreshToken = SecurityHelper.randomStringToken();
            this.minecraftAccessToken = SecurityHelper.randomStringToken();
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
        public String getMinecraftAccessToken() {
            return minecraftAccessToken;
        }

        @Override
        public long getExpireIn() {
            return expireMillis;
        }

        @Override
        public String toString() {
            return "UserSessionEntity{" +
                    "uuid=" + uuid +
                    ", entity=" + entity +
                    ", accessToken='" + accessToken + '\'' +
                    ", refreshToken='" + refreshToken + '\'' +
                    ", expireMillis=" + expireMillis +
                    '}';
        }
    }
}

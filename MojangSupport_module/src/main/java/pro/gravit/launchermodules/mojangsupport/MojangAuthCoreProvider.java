package pro.gravit.launchermodules.mojangsupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.user.UserSupportTextures;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class MojangAuthCoreProvider extends AuthCoreProvider {
    private static final Pattern UUID_REGEX = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    private transient final Logger logger = LogManager.getLogger();
    private transient HttpClient client;

    public static UUID getUUIDFromMojangHash(String hash) {
        return UUID.fromString(UUID_REGEX.matcher(hash).replaceFirst("$1-$2-$3-$4-$5"));
    }

    @Override
    public User getUserByUsername(String username) {
        try {
            MojangUUIDResponse response1 = mojangRequest("https://api.mojang.com/users/profiles/minecraft/%s".formatted(username), null, MojangUUIDResponse.class);
            if (response1 == null) {
                return null;
            }
            return getUserByHash(response1.id);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public User getUserByLogin(String login) {
        MojangUser user = new MojangUser();
        user.username = login;
        return user;
    }

    private MojangUser getUserByProfileResponse(MojangProfileResponse response1) {
        MojangUser user = new MojangUser();
        user.username = response1.name;
        user.uuid = getUUIDFromMojangHash(response1.id);
        var textures = response1.getTextures();
        if (textures != null) {
            for (var e : textures.textures.entrySet()) {
                if (e.getKey().equals("SKIN")) {
                    user.skin = e.getValue().toTexture();
                } else if (e.getKey().equals("CLOAK")) {
                    user.cloak = e.getValue().toTexture();
                }
            }
        }
        return user;
    }

    private MojangUser getUserByHash(String hash) {
        try {
            MojangProfileResponse response1 = mojangRequest("https://sessionserver.mojang.com/session/minecraft/profile/%s".formatted(hash), null, MojangProfileResponse.class);
            if (response1 == null) {
                return null;
            }
            return getUserByProfileResponse(response1);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            logger.error(e);
            return null;
        }
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        return getUserByHash(uuid.toString().replaceAll("-", ""));
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        try {
            MojangProfileByTokenResponse response1 = mojangRequest("https://api.minecraftservices.com/minecraft/profile", accessToken, MojangProfileByTokenResponse.class);
            if (response1 == null) {
                throw new OAuthAccessTokenExpired();
            }
            MojangUser user = new MojangUser();
            user.username = response1.name;
            user.uuid = getUUIDFromMojangHash(response1.id);
            user.skin = response1.skins.stream()
                    .filter(MojangProfileByTokenResponse.MojangProfileByTokenTextureResponse::isActive)
                    .findFirst()
                    .map(MojangProfileByTokenResponse.MojangProfileByTokenTextureResponse::toTexture)
                    .orElse(null);
            user.cloak = response1.capes.stream()
                    .filter(MojangProfileByTokenResponse.MojangProfileByTokenTextureResponse::isActive)
                    .findFirst()
                    .map(MojangProfileByTokenResponse.MojangProfileByTokenTextureResponse::toTexture)
                    .orElse(null);
            return new MojangUserSession(user, accessToken);
        } catch (IOException | URISyntaxException | InterruptedException e) {

            return null;
        }
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        return null;
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        if (login == null) {
            throw AuthException.userNotFound();
        }
        if (password == null) {
            throw AuthException.wrongPassword();
        }
        MojangAuthRequest request = new MojangAuthRequest(login, ((AuthPlainPassword) password).password);
        try {
            var result =
                    mojangRequest("POST", "https://authserver.mojang.com/authenticate", null, request, MojangAuthResponse.class);
            if (result == null) {
                throw AuthException.wrongPassword();
            }
            MojangUser mojangUser = new MojangUser();
            mojangUser.username = result.selectedProfile.username;
            mojangUser.uuid = getUUIDFromMojangHash(result.selectedProfile.id);
            MojangUser userWithProfile = getUserByHash(result.selectedProfile.id);
            if (userWithProfile != null) {
                mojangUser.username = userWithProfile.username;
                mojangUser.uuid = userWithProfile.uuid;
                mojangUser.skin = userWithProfile.skin;
                mojangUser.cloak = userWithProfile.cloak;
            }
            MojangUserSession session = new MojangUserSession(mojangUser, result.accessToken);
            return AuthManager.AuthReport.ofOAuth(result.accessToken, null, 0, session);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw AuthException.wrongPassword();
        }
    }

    @Override
    public void init(LaunchServer server) {
        client = HttpClient.newBuilder().build();
    }

    @Override
    public boolean joinServer(Client client, String username, UUID uuid, String accessToken, String serverID) {
        MojangUser user = (MojangUser) client.getUser();
        if (user == null) return false;
        MojangJoinServerRequest request;
        if(uuid == null) { // Before 1.20.2
            request = new MojangJoinServerRequest(accessToken, user.uuid, serverID);
        } else {
            request = new MojangJoinServerRequest(accessToken, uuid, serverID);
        }
        try {
            mojangRequest("POST", "https://sessionserver.mojang.com/session/minecraft/join", null, request, Void.class);
            return true;
        } catch (URISyntaxException | InterruptedException | IOException e) {
            return false;
        }
    }

    @Override
    public User checkServer(Client client, String username, String serverID) {
        String url = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s".formatted(username, serverID);
        try {
            MojangProfileResponse result = mojangRequest(url, null, MojangProfileResponse.class);
            if(result == null) {
                return null;
            }
            return getUserByProfileResponse(result);
        } catch (URISyntaxException | InterruptedException | IOException e) {
            logger.error(e);
            return null;
        }
    }

    protected <T> T mojangRequest(String url, String accessToken, Class<T> clazz) throws IOException, URISyntaxException, InterruptedException {
        return mojangRequest("GET", url, accessToken, null, clazz);
    }

    protected <T, V> T mojangRequest(String method, String url, String accessToken, V request, Class<T> clazz) throws IOException, URISyntaxException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .method(method, request == null ? HttpRequest.BodyPublishers.noBody() :
                        HttpRequest.BodyPublishers.ofString(Launcher.gsonManager.gson.toJson(request)))
                .uri(new URI(url))
                .header("Accept", "application/json");
        if (request != null) {
            builder = builder.header("Content-Type", "application/json");
        }
        if (accessToken != null) {
            builder = builder.header("Authorization", "Bearer ".concat(accessToken));
        }
        HttpRequest request1 = builder.build();
        HttpResponse<String> response = client.send(request1, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            String error = response.body();
            if (error != null && !error.isEmpty()) {
                logger.warn("Error {} return {}", url, error);
            }
            return null;
        }
        return Launcher.gsonManager.gson.fromJson(response.body(), clazz);
    }

    @Override
    public void close() {

    }

    private static class MojangUUIDResponse {
        public String name;
        public String id;
    }

    public static class AbstractMojangPropertiesResponse {
        public List<MojangProfileProperty> properties;

        public MojangProfilePropertyTexture getTextures() {
            MojangProfileProperty property = null;
            for (var e : properties) {
                if ("textures".equals(e.name)) {
                    property = e;
                    break;
                }
            }
            if (property == null) {
                return null;
            }
            String jsonData = new String(Base64.getDecoder().decode(property.value), StandardCharsets.UTF_8);
            return Launcher.gsonManager.gson.fromJson(jsonData, MojangProfileResponse.MojangProfilePropertyTexture.class);
        }

        public static class MojangProfileProperty {
            public String name;
            public String value;
            public String signature;
        }

        public static class MojangProfileTexture {
            public String url;
            public String digest;
            public Map<String, String> metadata;

            public Texture toTexture() {
                return new Texture(url, digest == null ? SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, url) : SecurityHelper.fromHex(digest), metadata);
            }
        }

        public static class MojangProfilePropertyTexture {
            public String profileId;
            public String profileName;
            public boolean signatureRequired;
            public Map<String, MojangProfileTexture> textures;
        }
    }

    public static class MojangProfileResponse extends AbstractMojangPropertiesResponse {
        public String id;
        public String name;
    }

    public static class MojangProfileByTokenResponse {
        public String id;
        public String name;
        public List<MojangProfileByTokenTextureResponse> skins;
        public List<MojangProfileByTokenTextureResponse> capes;

        public static class MojangProfileByTokenTextureResponse {
            public String id;
            public String state;
            public String url;
            public String digest;
            public String variant;

            public boolean isActive() {
                return "ACTIVE".equals(state);
            }

            public Texture toTexture() {
                Map<String, String> metadata = null;
                if (variant != null && variant.equals("SLIM")) {
                    metadata = Map.of("model", "slim");
                }
                return new Texture(url, digest == null ? SecurityHelper.digest(SecurityHelper.DigestAlgorithm.MD5, url) : SecurityHelper.fromHex(digest), metadata);
            }
        }
    }

    public static class MojangAgentRequest {
        public String name;
        public int version;
    }

    public static class MojangAuthRequest {
        public final MojangAgentRequest agent;
        public final String username;
        public final String password;

        public MojangAuthRequest(String username, String password) {
            this.username = username;
            this.password = password;
            agent = new MojangAgentRequest();
            agent.name = "Minecraft";
            agent.version = 1;
        }
    }

    public static class MojangAuthResponse {
        public MojangAuthUser user;
        public String clientToken;
        public String accessToken;
        public List<MojangAuthProfile> availableProfiles;
        public MojangAuthProfile selectedProfile;

        public static class MojangAuthProfile {
            public String username;
            public String id;
        }

        public static class MojangAuthUser { // Legacy user
            public String username;
            public List<AbstractMojangPropertiesResponse.MojangProfileProperty> properties;
            public String id;
        }
    }

    public static class MojangJoinServerRequest {
        public String accessToken;
        public String selectedProfile;
        public String serverId;

        public MojangJoinServerRequest(String accessToken, UUID uuid, String serverId) {
            this.accessToken = accessToken;
            this.selectedProfile = uuid.toString().replaceAll("-", "");
            this.serverId = serverId;
        }
    }

    public static class MojangUserSession implements UserSession {
        private final String id;
        private final MojangUser user;
        private final String accessToken;

        public MojangUserSession(MojangUser user, String accessToken) {
            this.id = SecurityHelper.randomStringToken();
            this.user = user;
            this.accessToken = accessToken;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public User getUser() {
            return user;
        }

        @Override
        public String getMinecraftAccessToken() {
            return accessToken;
        }

        @Override
        public long getExpireIn() {
            return 0;
        }

        @Override
        public String toString() {
            return "MojangUserSession{" +
                    "id='" + id + '\'' +
                    ", user=" + user +
                    ", accessToken='" + (accessToken == null ? "null" : "XXXXXX") + '\'' +
                    '}';
        }
    }

    public static class MojangError {
        public String error;
    }

    public static class MojangUser implements User, UserSupportTextures {
        private String username;
        private UUID uuid;
        private Texture skin;
        private Texture cloak;

        public MojangUser() {
        }

        public MojangUser(String username, UUID uuid, Texture skin, Texture cloak) {
            this.username = username;
            this.uuid = uuid;
            this.skin = skin;
            this.cloak = cloak;
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
        public ClientPermissions getPermissions() {
            return new ClientPermissions();
        }

        @Override
        public Texture getSkinTexture() {
            return skin;
        }

        @Override
        public Texture getCloakTexture() {
            return cloak;
        }

        @Override
        public String toString() {
            return "MojangUser{" +
                    "username='" + username + '\'' +
                    ", uuid=" + uuid +
                    ", skin=" + skin +
                    ", cloak=" + cloak +
                    '}';
        }
    }
}

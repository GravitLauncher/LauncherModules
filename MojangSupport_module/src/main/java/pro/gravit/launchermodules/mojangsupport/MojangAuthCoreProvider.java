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

    private static class MojangUUIDResponse {
        public String name;
        public String id;
    }

    @Override
    public User getUserByUsername(String username) {
        try {
            MojangUUIDResponse response1 = mojangRequest(String.format("https://api.mojang.com/users/profiles/minecraft/%s", username), null, MojangUUIDResponse.class);
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

    public static UUID getUUIDFromMojangHash(String hash) {
        return UUID.fromString(UUID_REGEX.matcher(hash).replaceFirst("$1-$2-$3-$4-$5"));
    }

    public static class AbstractMojangPropertiesResponse {
        public List<MojangProfileProperty> properties;

        public static class MojangProfileProperty {
            public String name;
            public String value;
            public String signature;
        }

        public static class MojangProfilePropertyTexture {
            public String profileId;
            public String profileName;
            public boolean signatureRequired;
            public Map<String, Texture> textures;
        }

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
    }

    public static class MojangProfileResponse extends AbstractMojangPropertiesResponse {
        public String id;
        public String name;
    }

    private MojangUser getUserByProfileResponse(MojangProfileResponse response1) {
        MojangUser user = new MojangUser();
        user.username = response1.name;
        user.uuid = getUUIDFromMojangHash(response1.id);
        var textures = response1.getTextures();
        if (textures != null) {
            for (var e : textures.textures.entrySet()) {
                if (e.getKey().equals("SKIN")) {
                    user.skin = e.getValue();
                } else if (e.getKey().equals("CLOAK")) {
                    user.cloak = e.getValue();
                }
            }
        }
        return user;
    }

    private MojangUser getUserByHash(String hash) {
        try {
            MojangProfileResponse response1 = mojangRequest(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s", hash), null, MojangProfileResponse.class);
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
                return new Texture(url, digest == null ? new byte[0] : SecurityHelper.fromHex(digest), metadata);
            }
        }
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
            user.accessToken = accessToken;
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
    public void verifyAuth(AuthResponse.AuthContext context) throws AuthException {

    }

    static class MojangPasswordVerifyReport extends PasswordVerifyReport {
        private final String accessToken;

        public MojangPasswordVerifyReport(String accessToken) {
            super(true);
            this.accessToken = accessToken;
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

    @Override
    public PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password) {
        if (!(password instanceof AuthPlainPassword)) {
            return PasswordVerifyReport.FAILED;
        }
        MojangUser mojangUser = (MojangUser) user;
        MojangAuthRequest request = new MojangAuthRequest(user.getUsername(), ((AuthPlainPassword) password).password);
        try {
            var result =
                    mojangRequest("POST", "https://authserver.mojang.com/authenticate", null, request, MojangAuthResponse.class);
            if (result == null) {
                return PasswordVerifyReport.FAILED;
            }
            mojangUser.username = result.selectedProfile.username;
            mojangUser.uuid = getUUIDFromMojangHash(result.selectedProfile.id);
            mojangUser.accessToken = result.accessToken;
            MojangUser userWithProfile = getUserByHash(result.selectedProfile.id);
            if (userWithProfile != null) {
                mojangUser.username = userWithProfile.username;
                mojangUser.uuid = userWithProfile.uuid;
                mojangUser.skin = userWithProfile.skin;
                mojangUser.cloak = userWithProfile.cloak;
            }
            return new MojangPasswordVerifyReport(result.accessToken);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            return PasswordVerifyReport.FAILED;
        }
    }

    @Override
    public AuthManager.AuthReport createOAuthSession(User user, AuthResponse.AuthContext context, PasswordVerifyReport report, boolean minecraftAccess) throws IOException {
        if (report == null) {
            throw new UnsupportedOperationException("Mojang authorization not be ignored");
        }
        MojangPasswordVerifyReport mojangReport = (MojangPasswordVerifyReport) report;
        MojangUser mojangUser = (MojangUser) user;
        MojangUserSession session = new MojangUserSession(mojangUser, mojangReport.accessToken);
        return AuthManager.AuthReport.ofOAuth(mojangReport.accessToken, null, 0, session);
    }

    @Override
    public void init(LaunchServer server) {
        client = HttpClient.newBuilder().build();
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        MojangUser mojangUser = (MojangUser) user;
        mojangUser.serverId = serverID;
        return false;
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

    @Override
    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        MojangUser user = (MojangUser) client.getUser();
        if (user == null) return false;
        MojangJoinServerRequest request = new MojangJoinServerRequest(accessToken, user.uuid, serverID);
        try {
            mojangRequest("POST", "https://sessionserver.mojang.com/session/minecraft/join", null, request, Void.class);
            return true;
        } catch (URISyntaxException | InterruptedException | IOException e) {
            return false;
        }
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        String url = String.format("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s", username, serverID);
        try {
            MojangProfileResponse result = mojangRequest(url, null, MojangProfileResponse.class);
            return getUserByProfileResponse(result);
        } catch (URISyntaxException | InterruptedException | IOException e) {
            logger.error(e);
            return null;
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

    protected <T> T mojangRequest(String url, String accessToken, Class<T> clazz) throws IOException, URISyntaxException, InterruptedException {
        return mojangRequest("GET", url, accessToken, null, clazz);
    }

    public static class MojangError {
        public String error;
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

    public static class MojangUser implements User, UserSupportTextures {
        private String username;
        private UUID uuid;
        private String accessToken;
        private String serverId;
        private Texture skin;
        private Texture cloak;

        public MojangUser() {
        }

        public MojangUser(String username, UUID uuid, String accessToken, String serverId, Texture skin, Texture cloak) {
            this.username = username;
            this.uuid = uuid;
            this.accessToken = accessToken;
            this.serverId = serverId;
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
        public String getServerId() {
            return serverId;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        @Override
        public ClientPermissions getPermissions() {
            return new ClientPermissions(0);
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
                    ", accessToken='" + (accessToken == null ? "null" : "XXXXXX") + '\'' +
                    ", serverId='" + serverId + '\'' +
                    ", skin=" + skin +
                    ", cloak=" + cloak +
                    '}';
        }
    }

    @Override
    public void close() throws IOException {

    }
}

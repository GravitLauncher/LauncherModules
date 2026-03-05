package pro.gravit.launchermodules.mojangsupport;

import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.base.request.RequestException;
import pro.gravit.launcher.base.request.auth.AuthRequest;
import pro.gravit.launcher.base.request.auth.details.AuthWebViewDetails;
import pro.gravit.launcher.base.request.auth.password.AuthCodePassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launcher.base.HttpHelper;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class MicrosoftAuthCoreProvider extends MojangAuthCoreProvider {
    private transient final HttpClient client = HttpClient.newBuilder().build();
    private transient final Logger logger = LogManager.getLogger();
    public String clientId = "d772766b-19b4-4f69-b353-989f890c5d3b";
    public String clientSecret;
    private transient MojangAuthCoreProvider provider;
    private transient LaunchServer server;

    public record DeviceCodeResponse(String device_code, String user_code, String verification_uri,
                                     String verification_uri_complete, long expires_in, long interval) {
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        // Use device-code flow to avoid redirect URI mismatches.
        String scope = "XboxLive.signin offline_access";
        try {
            var device = sendMicrosoftDeviceCodeRequest(scope);
            // pass verification URI (complete if available) as URL and device code marker as redirectUrl
            String url = device.verification_uri_complete != null ?
                    device.verification_uri_complete :
                    device.verification_uri;
            String redirectMarker = "device:".concat(device.device_code).concat(":").concat(device.user_code);
            client.setStaticProperty("microsoftDeviceCode", device.device_code);
            client.setStaticProperty("microsoftUserCode", device.user_code);
            return List.of(new AuthWebViewDetails(url, redirectMarker));
        } catch (IOException e) {
            logger.error("Device code request failed", e);
            return List.of();
        }
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        return super.getUserSessionByOAuthAccessToken(accessToken);
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        try {
            var result = sendMicrosoftOAuthRefreshTokenRequest(refreshToken);
            if (result == null) {
                return null;
            }
            var response = getMinecraftTokenByMicrosoftToken(result.access_token);
            var session = this.getUserSessionByOAuthAccessToken(response.access_token);
            return AuthManager.AuthReport.ofOAuth(response.access_token, result.refresh_token, response.expires_in, session);
        } catch (IOException e) {
            logger.error("Microsoft refresh failed", e);
            return null;
        } catch (OAuthAccessTokenExpired e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        if (password == null) {
            throw AuthException.wrongPassword();
        }
        AuthCodePassword codePassword = (AuthCodePassword) password;
        var uri = codePassword.uri;

        if (!uri.startsWith("device:")) {
            throw new AuthException("Only device code flow is supported");
        }

        String marker = uri.substring("device:".length());
        String deviceCode = marker.contains(":") ? marker.split(":", 2)[0] : marker;

        MicrosoftOAuthTokenResponse token = tryGetDeviceToken(deviceCode);
        if (token == null) throw new AuthException("Microsoft auth error: device token");

        try {
            var response = getMinecraftTokenByMicrosoftToken(token.access_token);
            var session = getUserSessionByOAuthAccessToken(response.access_token);
            if (minecraftAccess) {
                return AuthManager.AuthReport.ofOAuthWithMinecraft(response.access_token, response.access_token, token.refresh_token, response.expires_in, session);
            } else {
                return AuthManager.AuthReport.ofOAuth(response.access_token, token.refresh_token, response.expires_in, session);
            }
        } catch (OAuthAccessTokenExpired e) {
            throw new AuthException("Internal Auth Error: Token invalid");
        }
    }

    private void checkMinecraftOwnership(String xstsToken, String uhs) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(makeURI("https://api.minecraftservices.com/entitlements/mcstore"))
                .header("Authorization", "Bearer " + xstsToken)
                .GET()
                .build();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.debug("Minecraft entitlements: {}", response.body());
    }

    private MinecraftLoginWithXBoxResponse getMinecraftTokenByMicrosoftToken(String microsoftAccessToken) throws IOException {
        logger.debug("Getting XBox Live token...");
        var xboxLive = sendMicrosoftXBoxLiveRequest(microsoftAccessToken);
        logger.debug("XBox Live token obtained, UHS: {}", xboxLive.getUHS());
        logger.debug("Getting XSTS token...");
        var xsts = sendMicrosoftXSTSRequest(xboxLive.Token);
        logger.debug("XSTS token obtained, UHS: {}", xsts.getUHS());
        logger.debug("Getting Minecraft token...");
        checkMinecraftOwnership(xsts.Token, xsts.getUHS());
        return sendMinecraftLoginWithXBoxRequest(xsts.getUHS(), xsts.Token);
    }

    private DeviceCodeResponse sendMicrosoftDeviceCodeRequest(String scope) throws IOException {
        String body = new StringBuilder()
                .append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
                .append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8))
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        var e = HttpHelper.send(client, request, new MicrosoftErrorHandler<>(DeviceCodeResponse.class));
        return e.getOrThrow();
    }
    private MicrosoftOAuthTokenResponse tryGetDeviceToken(String deviceCode) throws IOException {
        String body = new StringBuilder()
                .append("grant_type=urn:ietf:params:oauth:grant-type:device_code")
                .append("&device_code=").append(URLEncoder.encode(deviceCode, StandardCharsets.UTF_8))
                .append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
                .toString();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try {
            var e = HttpHelper.send(client, request, new MicrosoftErrorHandler<>(MicrosoftOAuthTokenResponse.class));
            return e.getOrThrow();
        } catch (RequestException re) {
            String msg = re.toString();
            if (msg.contains("authorization_pending")) {
                throw new AuthException("Microsoft authorization not completed yet. Please finish sign-in in browser and try again.");
            } else if (msg.contains("expired_token")) {
                throw new AuthException("Device code expired. Please restart the login process.");
            } else {
                throw new IOException(re);
            }
        }
    }
    private MicrosoftOAuthTokenResponse pollDeviceToken(String deviceCode) throws IOException {
        long start = System.currentTimeMillis();
        long expires = System.currentTimeMillis() + 1000L * 60 * 10; // default 10 minutes fallback
        long interval = 5;
        // initial request to get interval/expiry if possible
        // but we don't have that here; poll until token or timeout
        while (System.currentTimeMillis() < expires) {
            String body = new StringBuilder()
                    .append("grant_type=urn:ietf:params:oauth:grant-type:device_code")
                    .append("&device_code=").append(URLEncoder.encode(deviceCode, StandardCharsets.UTF_8))
                    .append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
                    .toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            try {
                var e = HttpHelper.send(client, request, new MicrosoftErrorHandler<>(MicrosoftOAuthTokenResponse.class));
                var resp = e.getOrThrow();
                if (resp != null) return resp;
            } catch (RequestException re) {
                // parse error code from message if possible
                String msg = re.toString();
                if (msg.contains("authorization_pending")) {
                    // not ready yet
                } else if (msg.contains("slow_down")) {
                    interval += 5;
                } else if (msg.contains("expired_token")) {
                    return null;
                } else {
                    throw new IOException(re);
                }
            }
            try {
                Thread.sleep(interval * 1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while polling device token");
            }
        }
        throw new IOException("Device code polling timeout");
    }

    private MicrosoftOAuthTokenResponse sendMicrosoftOAuthRefreshTokenRequest(String refreshToken) throws IOException {
        String body = new StringBuilder()
                .append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8))
                .append(clientSecret != null ? "&client_secret=".concat(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)) : "")
                .append("&refresh_token=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                .append("&grant_type=refresh_token").toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        var e = HttpHelper.send(client, request, new MicrosoftErrorHandler<>(MicrosoftOAuthTokenResponse.class));
        return e.getOrThrow();
    }

    private MicrosoftXBoxLiveResponse sendMicrosoftXBoxLiveRequest(String accessToken) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(makeURI("https://user.auth.xboxlive.com/user/authenticate"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpHelper.jsonBodyPublisher(new MicrosoftXBoxLiveRequest(accessToken)))
                .build();
        var e = HttpHelper.send(client, request, new HttpHelper.BasicJsonHttpErrorHandler<>(MicrosoftXBoxLiveResponse.class));
        return e.getOrThrow();
    }

    private MicrosoftXBoxLiveResponse sendMicrosoftXSTSRequest(String xblToken) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(makeURI("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpHelper.jsonBodyPublisher(new MicrosoftXSTSRequest(xblToken)))
                .build();
        var e = HttpHelper.send(client, request, new XSTSErrorHandler<>(MicrosoftXBoxLiveResponse.class));
        return e.getOrThrow();
    }

    private MinecraftLoginWithXBoxResponse sendMinecraftLoginWithXBoxRequest(String uhs, String xstsToken) throws IOException {
        String identityToken = "XBL3.0 x=%s;%s".formatted(uhs, xstsToken);
        logger.debug("Identity token prefix: {}", identityToken.substring(0, Math.min(50, identityToken.length())));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(makeURI("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpHelper.jsonBodyPublisher(new MinecraftLoginWithXBoxRequest(uhs, xstsToken)))
                .build();
        var e = HttpHelper.send(client, request, new HttpHelper.BasicJsonHttpErrorHandler<>(MinecraftLoginWithXBoxResponse.class));
        return e.getOrThrow();
    }

    private URI makeURI(String s) throws IOException {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public record XSTSError(String Identity, long XErr, String Message, String Redirect) {
        @Override
        public String toString() {
            if (Message != null && !Message.isEmpty()) {
                return Message;
            }
            if (XErr == 2148916233L) {
                return "The account doesn't have an Xbox account.";
            }
            if (XErr == 2148916235L) {
                return "The account is from a country where Xbox Live is not available/banned";
            }
            if (XErr == 2148916238L) {
                return "The account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult";
            }
            return "XSTS error: %d".formatted(XErr);
        }
    }

    public record MicrosoftError(String error, String error_description, String correlation_id) {
        @Override
        public String toString() {
            return error_description;
        }
    }

    private static class XSTSErrorHandler<T> implements HttpHelper.HttpJsonErrorHandler<T, XSTSError> {
        private final Class<T> type;

        private XSTSErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, XSTSError> applyJson(JsonElement response, int statusCode) {
            if (statusCode < 200 || statusCode >= 300) {
                return new HttpHelper.HttpOptional<>(null, Launcher.gsonManager.gson.fromJson(response, XSTSError.class), statusCode);
            } else {
                return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(response, type), null, statusCode);
            }
        }
    }

    private static class MicrosoftErrorHandler<T> implements HttpHelper.HttpJsonErrorHandler<T, MicrosoftError> {
        private final Class<T> type;

        private MicrosoftErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, MicrosoftError> applyJson(JsonElement response, int statusCode) {
            if (statusCode < 200 || statusCode >= 300) {
                return new HttpHelper.HttpOptional<>(null, Launcher.gsonManager.gson.fromJson(response, MicrosoftError.class), statusCode);
            } else {
                return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(response, type), null, statusCode);
            }
        }
    }

    public record MicrosoftOAuthTokenResponse(String token_type, long expires_in, String scope, String access_token,
                                              String refresh_token, String user_id, String foci) {
    }

    public record MicrosoftXBoxLivePropertiesRequest(String AuthMethod, String SiteName, String RpsTicket) {
        public MicrosoftXBoxLivePropertiesRequest(String accessToken) {
            this("RPS", "user.auth.xboxlive.com", "d=".concat(accessToken));
        }
    }

    public record MicrosoftXBoxLiveRequest(MicrosoftXBoxLivePropertiesRequest Properties, String RelyingParty,
                                           String TokenType) {
        public MicrosoftXBoxLiveRequest(String accessToken) {
            this(new MicrosoftXBoxLivePropertiesRequest(accessToken), "http://auth.xboxlive.com", "JWT");
        }
    }

    public record MicrosoftXBoxLiveResponse(String IssueInstant, String NotAfter, String Token,
                                            Map<String, List<Map<String, String>>> DisplayClaims) { //XBox Live and XSTS
        public String getUHS() {
            return DisplayClaims.get("xui").getFirst().get("uhs");
        }
    }

    public record MicrosoftXSTSPropertiesRequest(String SandboxId, List<String> UserTokens) {
        public MicrosoftXSTSPropertiesRequest(String xblToken) {
            this("RETAIL", List.of(xblToken));
        }
    }

    public record MicrosoftXSTSRequest(MicrosoftXSTSPropertiesRequest Properties, String RelyingParty,
                                       String TokenType) {
        public MicrosoftXSTSRequest(String xblToken) {
            this(new MicrosoftXSTSPropertiesRequest(xblToken), "rp://api.minecraftservices.com/", "JWT");
        }
    }

    public record MinecraftLoginWithXBoxRequest(String identityToken) {
        public MinecraftLoginWithXBoxRequest(String uhs, String xstsToken) {
            this("XBL3.0 x=%s;%s".formatted(uhs, xstsToken));
        }
    }

    public record MinecraftLoginWithXBoxResponse(String username, List<String> roles, String access_token,
                                                 String token_type, long expires_in) {
    }
}

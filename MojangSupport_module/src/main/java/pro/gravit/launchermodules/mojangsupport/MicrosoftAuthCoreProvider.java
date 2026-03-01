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
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.QueryHelper;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MicrosoftAuthCoreProvider extends MojangAuthCoreProvider {
    private static final String AUTH_CODE_URL = "https://login.live.com/oauth20_authorize.srf?client_id=%s&response_type=code&redirect_uri=%s&scope=XboxLive.signin offline_access";
    private transient final HttpClient client = HttpClient.newBuilder().build();
    private transient final Logger logger = LogManager.getLogger();
    public String redirectUrl = "https://login.live.com/oauth20_desktop.srf";
    public String clientId = "00000000402b5328";
    public String clientSecret;
    private transient MojangAuthCoreProvider provider;
    private transient LaunchServer server;

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        String uuid = UUID.randomUUID().toString();
        client.setStaticProperty("microsoftCode", uuid);
        return List.of(new AuthWebViewDetails(
                AUTH_CODE_URL.formatted(clientId, redirectUrl.formatted(uuid)),
                redirectUrl.formatted(uuid)
        ));
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
            return AuthManager.AuthReport.ofOAuth(response.access_token, result.refresh_token, SECONDS.toMillis(response.expires_in), null);
        } catch (IOException e) {
            logger.error("Microsoft refresh failed", e);
            return null;
        }
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws IOException {
        if (password == null) {
            throw AuthException.wrongPassword();
        }
        AuthCodePassword codePassword = (AuthCodePassword) password;
        var uri = URI.create(codePassword.uri);
        var queries = QueryHelper.splitUriQuery(uri);
        var code = CommonHelper.multimapFirstOrNullValue("code", queries);
        try {
            var token = sendMicrosoftOAuthTokenRequest(code);
            if (token == null) {
                throw new AuthException("Microsoft auth error: oauth token");
            }
            try {
                var response = getMinecraftTokenByMicrosoftToken(token.access_token);
                var session = getUserSessionByOAuthAccessToken(response.access_token);
                if (minecraftAccess) {
                    return AuthManager.AuthReport.ofOAuthWithMinecraft(response.access_token, response.access_token, token.refresh_token, SECONDS.toMillis(response.expires_in), session);
                } else {
                    return AuthManager.AuthReport.ofOAuth(response.access_token, token.refresh_token, SECONDS.toMillis(response.expires_in), session);
                }
            } catch (OAuthAccessTokenExpired e) {
                throw new AuthException("Internal Auth Error: Token invalid");
            }
        } catch (RequestException e) {
            throw new AuthException(e.toString());
        }
    }

    private MinecraftLoginWithXBoxResponse getMinecraftTokenByMicrosoftToken(String microsoftAccessToken) throws IOException {
        // XBox Live
        var xboxLive = sendMicrosoftXBoxLiveRequest(microsoftAccessToken);
        // XSTS
        var xsts = sendMicrosoftXSTSRequest(xboxLive.Token);
        // Minecraft auth
        return sendMinecraftLoginWithXBoxRequest(xsts.getUHS(), xsts.Token);
    }

    private URI makeOAuthTokenRequestURI(String code) throws IOException {
        StringBuilder builder = new StringBuilder("https://login.live.com/oauth20_token.srf?");

        builder.append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        if (clientSecret != null)
            builder.append("&client_secret=").append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8));
        builder.append("&code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8));
        builder.append("&grant_type=authorization_code");
        builder.append("&redirect_uri=").append(URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8));

        try {
            return new URI(builder.toString());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private String makeOAuthRefreshTokenRequestBody(String refreshToken) {
        StringBuilder builder = new StringBuilder();

        builder.append("client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        if (clientSecret != null)
            builder.append("&client_secret=").append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8));
        builder.append("&refresh_token=").append(URLEncoder.encode(refreshToken, StandardCharsets.UTF_8));
        builder.append("&grant_type=refresh_token");

        return builder.toString();
    }

    private MicrosoftOAuthTokenResponse sendMicrosoftOAuthTokenRequest(String code) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(makeOAuthTokenRequestURI(code))
                .GET()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        var e = HttpHelper.send(client, request, new MicrosoftErrorHandler<>(MicrosoftOAuthTokenResponse.class));
        return e.getOrThrow();
    }

    private MicrosoftOAuthTokenResponse sendMicrosoftOAuthRefreshTokenRequest(String refreshToken) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.live.com/oauth20_token.srf"))
                .POST(HttpRequest.BodyPublishers.ofString(makeOAuthRefreshTokenRequestBody(refreshToken)))
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

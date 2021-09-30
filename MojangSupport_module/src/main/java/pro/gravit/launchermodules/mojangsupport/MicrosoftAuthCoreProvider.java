package pro.gravit.launchermodules.mojangsupport;

import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthWebViewDetails;
import pro.gravit.launcher.request.auth.password.AuthCodePassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.helper.HttpHelper;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MicrosoftAuthCoreProvider extends MojangAuthCoreProvider {
    private transient MojangAuthCoreProvider provider;
    private transient LaunchServer server;
    private transient final HttpClient client = HttpClient.newBuilder().build();
    private transient final Logger logger = LogManager.getLogger();
    public String redirectUrl = "https://login.live.com/oauth20_desktop.srf";
    public String clientId = "00000000402b5328";
    public String clientSecret;
    private static final String AUTH_CODE_URL = "https://login.live.com/oauth20_authorize.srf?client_id=%s&response_type=code&redirect_uri=%s&scope=XboxLive.signin offline_access";

    public record XSTSError(String Identity, long XErr, String Message, String Redirect) {
        @Override
        public String toString() {
            if(Message != null && !Message.isEmpty()) {
                return Message;
            }
            if(XErr == 2148916233L) {
                return "The account doesn't have an Xbox account.";
            }
            if(XErr == 2148916235L) {
                return "The account is from a country where Xbox Live is not available/banned";
            }
            if(XErr == 2148916238L) {
                return "The account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult";
            }
            return String.format("XSTS error: %d", XErr);
        }
    }

    private static class XSTSErrorHandler<T> implements HttpHelper.HttpJsonErrorHandler<T, XSTSError> {
        private final Class<T> type;

        private XSTSErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, XSTSError> applyJson(JsonElement response, int statusCode) {
            if(statusCode < 200 || statusCode >= 300) {
                return new HttpHelper.HttpOptional<>(null, Launcher.gsonManager.gson.fromJson(response, XSTSError.class), statusCode);
            } else {
                return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(response, type), null, statusCode);
            }
        }
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        String uuid = UUID.randomUUID().toString();
        client.setSerializableProperty("microsoftCode", uuid);
        return List.of(new AuthWebViewDetails(
                String.format(AUTH_CODE_URL, clientId, String.format(redirectUrl, uuid)),
                String.format(redirectUrl, uuid)
        ));
    }

    /*@Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        try {
            var result = sendMicrosoftOAuthRefreshTokenRequest(refreshToken);
            if(result == null) {
                return null;
            }
            return AuthManager.AuthReport.ofOAuth(result.access_token, result.refresh_token, result.expires_in);
        } catch (IOException e) {
            logger.error("Microsoft refresh failed", e);
            return null;
        }
    }*/

    @Override
    public PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password) {
        return new MicrosoftPasswordVerifyReport(((AuthCodePassword)password).code);
    }

    private static class MicrosoftPasswordVerifyReport extends PasswordVerifyReport {
        private final String microsoftCode;

        public MicrosoftPasswordVerifyReport(String code) {
            super(true);
            this.microsoftCode = code;
        }
    }

    @Override
    public AuthManager.AuthReport createOAuthSession(User user, AuthResponse.AuthContext context, PasswordVerifyReport report, boolean minecraftAccess) throws IOException {
        if(report == null) {
            throw new AuthException("Microsoft authorization not be ignored");
        }
        var code = ((MicrosoftPasswordVerifyReport)report).microsoftCode;
        var token = sendMicrosoftOAuthTokenRequest(code);
        if(token == null) {
            throw new AuthException("Microsoft auth error: oauth token");
        }
        if(minecraftAccess) {

            try {
                return AuthManager.AuthReport.ofOAuthWithMinecraft(getMinecraftTokenByMicrosoftToken(token.access_token), token.access_token, token.refresh_token, token.expires_in, getUserSessionByOAuthAccessToken(token.access_token));
            } catch (OAuthAccessTokenExpired e) {
                throw new IOException(e);
            }
        } else {
            return AuthManager.AuthReport.ofOAuth(token.access_token, token.refresh_token, token.expires_in);
        }
    }

    private String getMinecraftTokenByMicrosoftToken(String microsoftAccessToken) throws IOException {
        // XBox Live
        var xboxLive = sendMicrosoftXBoxLiveRequest(microsoftAccessToken);
        // XSTS
        var xsts = sendMicrosoftXSTSRequest(xboxLive.Token);
        // Minecraft auth
        var token = sendMinecraftLoginWithXBoxRequest(xsts.getUHS(), xsts.Token);
        return token.access_token;
    }

    private URI makeOAuthTokenRequestURI(String code) throws IOException {
        URI uri;
        try {
            if(clientSecret != null) {
                uri = new URI(String.format("https://login.live.com/oauth20_token.srf?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code", clientId, clientSecret, code));
            } else {
                uri = new URI(String.format("https://login.live.com/oauth20_token.srf?client_id=%s&code=%s&grant_type=authorization_code", clientId, code));
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return uri;
    }

    private URI makeOAuthRefreshTokenRequestURI(String refreshToken) throws IOException {
        URI uri;
        try {
            if(clientSecret != null) {
                uri = new URI(String.format("https://login.live.com/oauth20_token.srf?client_id=%s&client_secret=%s&refresh_token=%s&grant_type=refresh_token", clientId, clientSecret, refreshToken));
            } else {
                uri = new URI(String.format("https://login.live.com/oauth20_token.srf?client_id=%s&refresh_token=%s&grant_type=refresh_token", clientId, refreshToken));
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return uri;
    }

    private MicrosoftOAuthTokenResponse sendMicrosoftOAuthTokenRequest(String code) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(makeOAuthTokenRequestURI(code))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        var e = HttpHelper.send(client, request, new HttpHelper.BasicJsonHttpErrorHandler<>(MicrosoftOAuthTokenResponse.class));
        return e.getOrThrow();
    }

    private MicrosoftOAuthTokenResponse sendMicrosoftOAuthRefreshTokenRequest(String refreshToken) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(makeOAuthRefreshTokenRequestURI(refreshToken))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        var e = HttpHelper.send(client, request, new HttpHelper.BasicJsonHttpErrorHandler<>(MicrosoftOAuthTokenResponse.class));
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

    public record MicrosoftOAuthTokenResponse(String token_type, long expires_in, String scope, String access_token, String refresh_token, String user_id, String foci) {}

    public record MicrosoftXBoxLivePropertiesRequest(String AuthMethod, String SiteName, String RpsTicket) {
        public MicrosoftXBoxLivePropertiesRequest(String accessToken) {
            this("RPC", "user.auth.xboxlive.com", "d=".concat(accessToken));
        }
    }

    public record MicrosoftXBoxLiveRequest(MicrosoftXBoxLivePropertiesRequest Properties, String RelyingParty, String TokenType) {
        public MicrosoftXBoxLiveRequest(String accessToken) {
            this(new MicrosoftXBoxLivePropertiesRequest(accessToken), "http://auth.xboxlive.com", "JWT");
        }
    }

    public record MicrosoftXBoxLiveResponse(String IssueInstant, String NotAfter, String Token, Map<String, List<Map<String, String>>> DisplayClaims) { //XBox Live and XSTS
        public String getUHS() {
            return DisplayClaims.get("xui").get(0).get("uhs");
        }
    }

    public record MicrosoftXSTSPropertiesRequest(String SandboxId, List<String> UserTokens) {
        public MicrosoftXSTSPropertiesRequest(String xblToken) {
            this("RETAIL", List.of(xblToken));
        }
    }

    public record MicrosoftXSTSRequest(MicrosoftXSTSPropertiesRequest Properties, String RelyingParty, String TokenType) {
        public MicrosoftXSTSRequest(String xblToken) {
            this(new MicrosoftXSTSPropertiesRequest(xblToken), "rp://api.minecraftservices.com/", "JWT");
        }
    }

    public record MinecraftLoginWithXBoxRequest(String identityToken) {
        public MinecraftLoginWithXBoxRequest(String uhs, String xstsToken) {
            this(String.format("XBL3.0 x=%s;%s", uhs, xstsToken));
        }
    }

    public record MinecraftLoginWithXBoxResponse(String username, List<String> roles, String access_token, String token_type, String expires_in) {}

    @Override
    public void close() throws IOException {

    }
}

package pro.gravit.launchermodules.mojangsupport;

import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthWebViewDetails;
import pro.gravit.launcher.request.auth.password.AuthCodePassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.AuthSocialProvider;
import pro.gravit.launchserver.auth.core.JsonCoreProvider;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;

public class MicrosoftAuthSocialProvider extends AuthSocialProvider { // Work in progress
    private transient MojangAuthCoreProvider provider;
    private transient LaunchServer server;
    private transient HttpClient client = HttpClient.newBuilder().build();
    public String redirectUrl = "http://localhost/code/%s/";
    public String clientId = "";
    public String clientSecret = "";
    private static final String AUTH_CODE_URL = "https://login.live.com/oauth20_authorize.srf?client_id=%s&response_type=code&redirect_uri=%s&scope=XboxLive.signin offline_access";
    @Override
    public void init(LaunchServer server, AuthCoreProvider provider) {
        this.server = server;
        this.provider = (MojangAuthCoreProvider) provider;
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

    @Override
    public SocialResult preAuth(AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password) throws AuthException {
        String code = ((AuthCodePassword)password).code;
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}

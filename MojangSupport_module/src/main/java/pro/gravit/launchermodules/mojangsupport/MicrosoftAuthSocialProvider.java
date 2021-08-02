package pro.gravit.launchermodules.mojangsupport;

import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.AuthSocialProvider;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.util.List;

public class MicrosoftAuthSocialProvider extends AuthSocialProvider { // Work in progress
    private transient MojangAuthCoreProvider provider;
    private transient LaunchServer server;

    @Override
    public void init(LaunchServer server, AuthCoreProvider provider) {
        this.server = server;
        this.provider = (MojangAuthCoreProvider) provider;
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        return null;
    }

    @Override
    public SocialResult preAuth(AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password) throws AuthException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}

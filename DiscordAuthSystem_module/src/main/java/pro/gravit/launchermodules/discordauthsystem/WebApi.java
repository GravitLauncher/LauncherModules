package pro.gravit.launchermodules.discordauthsystem;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.AdditionalDataRequestEvent;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordApi;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordSystemAuthCoreProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class WebApi implements NettyWebAPIHandler.SimpleSeverletHandler {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private final ModuleImpl module;
    private final LaunchServer server;
    private transient final Logger logger = LogManager.getLogger();

    public WebApi(ModuleImpl module, LaunchServer server) {
        this.module = module;
        this.server = server;
    }

    public static String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        return NONLATIN.matcher(normalized).replaceAll("");
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest msg, NettyConnectContext context) throws Exception {
        //TODO: Переписать этот блок под новую реализацию
        Map<String, String> params = getParamsFromUri(msg.uri());

        String state = params.get("state");

        if (state == null || state.isEmpty()) {
            sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.NOT_FOUND, "The \"state\" parameter was not found."));
            return;
        }

        String code = params.get("code");

        if (code == null || code.isEmpty()) {
            sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.NOT_FOUND, "The \"code\" parameter was not found."));
            return;
        }

        var accessTokenResponse = DiscordApi.getAccessTokenByCode(code);

        var response = DiscordApi.getDiscordUserByAccessToken(accessTokenResponse.access_token);

        AuthProviderPair pair = server.config.getAuthProviderPair();
        DiscordSystemAuthCoreProvider core = (DiscordSystemAuthCoreProvider) pair.core;

        DiscordSystemAuthCoreProvider.DiscordUser user = core.getUserByDiscordId(response.user.id);
        if (user == null) {
            String username = toSlug(response.user.username);

            if (username.length() == 0) {
                sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.NOT_ACCEPTABLE, "Your nickname does not meet the requirements. Please change it."));
                return;
            }

            if (core.getUserByUsername(username) != null) {
                username += "_" + response.user.discriminator;
            }
            user = core.createUser(
                    state,
                    username,
                    accessTokenResponse.access_token,
                    accessTokenResponse.refresh_token,
                    accessTokenResponse.expires_in * 1000,
                    response.user.id
            );
        } else {
            user = core.updateDataUser(response.user.id, accessTokenResponse.access_token, accessTokenResponse.refresh_token, accessTokenResponse.expires_in * 1000);
        }
        if (user.isBanned()) {
            sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.FORBIDDEN, "You have been banned!"));
            return;
        }

        String minecraftAccessToken;
        AuthRequestEvent.OAuthRequestEvent oauth;

        AuthManager.AuthReport report = pair.core.authorize(user.getUsername(), null, null, true);
        minecraftAccessToken = report.minecraftAccessToken();
        oauth = new AuthRequestEvent.OAuthRequestEvent(report.oauthAccessToken(), report.oauthRefreshToken(), report.oauthExpire());

        DiscordSystemAuthCoreProvider.DiscordUser finalUser = user;
        server.nettyServerSocketHandler.nettyServer.service.forEachActiveChannels((ch, ws) -> {

            Client client = ws.getClient();
            if (client == null) {
                return;
            }
            String wsState = client.getProperty("state");
            if (wsState == null || wsState.isEmpty() || !wsState.equals(state)) {
                return;
            }

            client.coreObject = finalUser;
            client.sessionObject = report.session();
            server.authManager.internalAuth(client, AuthResponse.ConnectTypes.CLIENT, pair, finalUser.getUsername(), finalUser.getUUID(), ClientPermissions.DEFAULT, true);
            PlayerProfile playerProfile = server.authManager.getPlayerProfile(client);
            AuthRequestEvent request = new AuthRequestEvent(ClientPermissions.DEFAULT, playerProfile, minecraftAccessToken, null, null, oauth);
            request.requestUUID = RequestEvent.eventUUID;

            server.nettyServerSocketHandler.nettyServer.service.sendObject(ch, request);

            Map<String, String> data = new HashMap<String, String>();

            data.put("type", "ChangeRuntimeSettings");
            data.put("login", finalUser.getUsername());
            data.put("oauthAccessToken", finalUser.getAccessToken());
            data.put("oauthRefreshToken", finalUser.getRefreshToken());
            data.put("oauthExpire", finalUser.getExpiresIn().toString());

            AdditionalDataRequestEvent dataRequestEvent = new AdditionalDataRequestEvent(data);
            dataRequestEvent.requestUUID = RequestEvent.eventUUID;

            server.nettyServerSocketHandler.nettyServer.service.sendObject(ch, dataRequestEvent);
        });
        sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.OK, "You are successfully authorized! Please return to the launcher."));
    }
}

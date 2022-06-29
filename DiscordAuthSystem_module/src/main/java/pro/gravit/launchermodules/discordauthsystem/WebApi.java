package pro.gravit.launchermodules.discordauthsystem;

import com.github.slugify.Slugify;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.AdditionalDataRequestEvent;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.request.auth.password.AuthCodePassword;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordApi;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordSystemAuthCoreProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.File;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class WebApi implements NettyWebAPIHandler.SimpleSeverletHandler {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private final ModuleImpl module;
    private final LaunchServer server;
    private transient final Logger logger = LogManager.getLogger();
    private final Slugify slg = Slugify.builder().underscoreSeparator(true).lowerCase(false).transliterator(true).build();

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
        Map<String, String> params = getParamsFromUri(msg.uri());

        String state = params.get("state");

        if (state == null || state.isEmpty()) {
            sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.NOT_FOUND, "The \"state\" parameter was not found."));
            return;
        }

        AtomicBoolean userFined = new AtomicBoolean(false);

        server.nettyServerSocketHandler.nettyServer.service.forEachActiveChannels((ch, ws) -> {

            Client client = ws.getClient();
            if (client == null) {
                return;
            }

            String wsState = client.getProperty("state");
            if (wsState == null || wsState.isEmpty() || !wsState.equals(state)) {
                return;
            }

            userFined.set(true);
        });

        if (!userFined.get()) {
            sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.NOT_FOUND, "The \"state\" parameter is invalid."));
            return;
        }

        String code = params.get("code");

        if (code == null || code.isEmpty()) {
            sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.NOT_FOUND, "The \"code\" parameter was not found."));
            return;
        }

        AuthProviderPair pair = server.config.getAuthProviderPair();
        AuthManager.AuthReport report;

        try {
            report = pair.core.authorize("", null, new AuthCodePassword(code), true);
        } catch (AuthException e) {
            sendHttpResponse(ctx, simpleHtmlResponse(HttpResponseStatus.FORBIDDEN, e.getMessage()));
            return;
        }

        String minecraftAccessToken = report.minecraftAccessToken();
        AuthRequestEvent.OAuthRequestEvent oauth = new AuthRequestEvent.OAuthRequestEvent(report.oauthAccessToken(), report.oauthRefreshToken(), report.oauthExpire());

        DiscordSystemAuthCoreProvider.DiscordUser user = (DiscordSystemAuthCoreProvider.DiscordUser) report.session().getUser();

        server.nettyServerSocketHandler.nettyServer.service.forEachActiveChannels((ch, ws) -> {

            Client client = ws.getClient();
            if (client == null) {
                return;
            }

            String wsState = client.getProperty("state");
            if (wsState == null || wsState.isEmpty() || !wsState.equals(state)) {
                return;
            }

            client.coreObject = user;
            client.sessionObject = report.session();
            server.authManager.internalAuth(client, AuthResponse.ConnectTypes.CLIENT, pair, user.getUsername(), user.getUUID(), ClientPermissions.DEFAULT, true);
            PlayerProfile playerProfile = server.authManager.getPlayerProfile(client);
            AuthRequestEvent request = new AuthRequestEvent(ClientPermissions.DEFAULT, playerProfile, minecraftAccessToken, null, null, oauth);
            request.requestUUID = RequestEvent.eventUUID;

            server.nettyServerSocketHandler.nettyServer.service.sendObject(ch, request);
        });
        sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.OK, "You are successfully authorized! Please return to the launcher."));
    }

    public FullHttpResponse simpleHtmlResponse(HttpResponseStatus status, String body) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.setStatus(status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        StringBuilder buf = new StringBuilder()
                .append("<!DOCTYPE html>\r\n")
                .append("<html><head><meta charset='utf-8' /></head>")
                .append("<body>\r\n")
                .append(body)
                .append("</body></html>\r\n");

        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        return response;
    }
}

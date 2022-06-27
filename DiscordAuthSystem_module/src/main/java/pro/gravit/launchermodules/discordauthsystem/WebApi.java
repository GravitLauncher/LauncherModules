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
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordApi;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordSystemAuthCoreProvider;
import pro.gravit.launchserver.LaunchServer;
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

        DiscordApi.DiscordAccessTokenResponse accessTokenResponse = null;

        try {
            accessTokenResponse = DiscordApi.getAccessTokenByCode(code);
        } catch (Exception e) {
            sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.FORBIDDEN, "Discord authorization denied your code."));
            return;
        }

        var response = DiscordApi.getDiscordUserByAccessToken(accessTokenResponse.access_token);

        if (!module.config.guildIdsJoined.isEmpty()) {
            var guilds = DiscordApi.getUserGuilds(accessTokenResponse.access_token);

            var needGuilds = module.config.guildIdsJoined;

            for (var guild : guilds) {
                needGuilds.removeIf(g -> Objects.equals(g.id, guild.id));
            }

            if (!needGuilds.isEmpty()) {
                String body = "To enter the server you must be a member of these guilds: ";
                List<String> guildData = new ArrayList<>();
                for (var g : needGuilds) {
                    guildData.add("<a href=\"" + g.url + "\">" + g.name + "</a>");
                }
                sendHttpResponse(ctx, simpleHtmlResponse(HttpResponseStatus.FORBIDDEN, body + String.join(", ", guildData)));
                return;
            }
        }

        AuthProviderPair pair = server.config.getAuthProviderPair();
        DiscordSystemAuthCoreProvider core = (DiscordSystemAuthCoreProvider) pair.core;

        DiscordSystemAuthCoreProvider.DiscordUser user = core.getUserByDiscordId(response.user.id);

        if (user == null) {
            String username = response.user.username;
            if (module.config.guildIdGetNick.length() > 0) {
                try {
                    var member = DiscordApi.getUserGuildMember(accessTokenResponse.access_token, module.config.guildIdGetNick);
                    if (member.nick != null) {
                        username = member.nick;
                    }
                } catch (Exception e) {
                    logger.error("DiscordApi.getUserGuildMember: " + e);
                    sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred!"));
                    return;
                }
            }

            username = slg.slugify(username);

            var usernameLength = username.length();

            if (usernameLength == 0) {
                sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.NOT_ACCEPTABLE, "Your nickname does not meet the requirements. Please change it."));
                return;
            }

            if (module.config.usernameRegex.length() > 0) {
                if (!username.matches(module.config.usernameRegex)) {
                    sendHttpResponse(ctx, simpleResponse(HttpResponseStatus.NOT_ACCEPTABLE, "Your nickname does not meet the requirements. Please change it."));
                    return;
                }
            }

            if (core.getUserByUsername(username) != null) {
                username = username.substring(0, usernameLength-1-response.user.discriminator.length());
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

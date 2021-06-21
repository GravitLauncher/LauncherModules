package pro.gravit.launchermodules.unsafecommands.commands;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.RequiredDAO;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.launchserver.command.Command;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.launchserver.socket.response.profile.ProfileByUUIDResponse;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.util.UUID;

public class SendAuthCommand extends Command {
    public SendAuthCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[connectUUID] [username] [auth_id] [client type] (permissions)";
    }

    @Override
    public String getUsageDescription() {
        return "manual send auth request";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 4);
        UUID connectUUID = parseUUID(args[0]);
        String username = args[1];
        AuthResponse.ConnectTypes type = AuthResponse.ConnectTypes.valueOf(args[3]);
        AuthProviderPair pair = server.config.getAuthProviderPair(args[2]);
        ClientPermissions permissions = args.length > 4 ? new ClientPermissions(Long.parseLong(args[4])) : ClientPermissions.DEFAULT;
        if(pair.isUseCore()) {
            User user = pair.core.getUserByLogin(username);
            UUID uuid;
            if(user == null) {
                uuid = UUID.randomUUID();
            } else {
                uuid = user.getUUID();
            }
            UserSession session;
            String minecraftAccessToken;
            AuthRequestEvent.OAuthRequestEvent oauth;
            if(user != null) {
                AuthManager.AuthReport report = pair.core.createOAuthSession(user, null, null, true);
                if(report == null) throw new UnsupportedOperationException("AuthCoreProvider not supported sendAuth");
                minecraftAccessToken = report.minecraftAccessToken;

                if(report.isUsingOAuth()) {
                    session = report.session;
                    oauth = new AuthRequestEvent.OAuthRequestEvent(report.oauthAccessToken, report.oauthRefreshToken, report.oauthExpire);
                } else {
                    session = null;
                    oauth = null;
                }
            } else {
                session = null;
                minecraftAccessToken = null;
                oauth = null;
            }
            server.nettyServerSocketHandler.nettyServer.service.forEachActiveChannels((ch, ws) -> {
                if(!ws.getConnectUUID().equals(connectUUID)) return;
                Client client = ws.getClient();
                client.coreObject = user;
                client.sessionObject = session;
                server.authManager.internalAuth(client, type, pair, username, uuid, permissions, oauth != null);
                PlayerProfile playerProfile = server.authManager.getPlayerProfile(client);
                server.nettyServerSocketHandler.nettyServer.service.sendObject(ch, new AuthRequestEvent(permissions, playerProfile, minecraftAccessToken, null, oauth == null ? client.session : null, oauth));
            });
        } else {
            throw new UnsupportedOperationException("Auth provider/handler not supported");
        }
    }
}

package pro.gravit.launchermodules.unsafecommands.commands;

import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthProviderPair;
import pro.gravit.launchserver.auth.RequiredDAO;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.launchserver.command.Command;
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
        return "[connectUUID] [username] [auth_id] [client type] (permissions) (client uuid)";
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

        UUID clientUUID;
        String accessToken = SecurityHelper.randomStringToken();
        if (pair == null) {
            clientUUID = args.length > 5 ? UUID.fromString(args[5]) : UUID.randomUUID();
        } else {
            clientUUID = pair.handler.auth(new AuthProviderResult(username, accessToken, permissions));
        }
        WebSocketService service = server.nettyServerSocketHandler.nettyServer.service;
        service.channels.forEach((channel -> {
            WebSocketFrameHandler frameHandler = channel.pipeline().get(WebSocketFrameHandler.class);
            if (frameHandler.getConnectUUID().equals(connectUUID)) {
                Client client = frameHandler.getClient();
                if (client.isAuth) LogHelper.warning("This client already authorized");
                client.isAuth = true;
                client.username = username;
                if (pair != null) {
                    client.auth_id = args[3];
                    client.auth = pair;
                    if (pair.provider instanceof RequiredDAO || pair.handler instanceof RequiredDAO) {
                        client.daoObject = server.config.dao.userDAO.findByUsername(username);
                    }
                }
                client.type = type;
                client.permissions = permissions;
                client.session = UUID.randomUUID();
                server.sessionManager.addClient(client);
                PlayerProfile playerProfile = ProfileByUUIDResponse.getProfile(clientUUID, username, "client", pair != null ? pair.textureProvider : server.config.getAuthProviderPair().textureProvider);
                AuthRequestEvent authRequestEvent = new AuthRequestEvent(playerProfile, accessToken, permissions);
                authRequestEvent.requestUUID = RequestEvent.eventUUID;
                authRequestEvent.session = client.session;
                service.sendObject(channel, authRequestEvent);
                LogHelper.info("Success auth injected");
            }
        }));
    }
}

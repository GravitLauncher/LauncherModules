package pro.gravit.launchermodules.discordauthsystem.responses;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.events.RequestEvent;
import pro.gravit.launcher.events.request.ExitRequestEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.handlers.WebSocketFrameHandler;

public class ExitResponse extends pro.gravit.launchserver.socket.response.auth.ExitResponse {

    public static void exit(LaunchServer server, WebSocketFrameHandler wsHandler, Channel channel, ExitRequestEvent.ExitReason reason) {
        Client chClient = wsHandler.getClient();
        Client newCusClient = new Client();
        newCusClient.setProperty("state", chClient.getProperty("state"));
        newCusClient.checkSign = chClient.checkSign;
        wsHandler.setClient(newCusClient);
        ExitRequestEvent event = new ExitRequestEvent(reason);
        event.requestUUID = RequestEvent.eventUUID;
        wsHandler.service.sendObject(channel, event);
    }

    @Override
    public void execute(ChannelHandlerContext ctx, Client client) {
        var state = client.getProperty("state");
        super.execute(ctx, client);
        if (username == null) {
            WebSocketFrameHandler handler = ctx.pipeline().get(WebSocketFrameHandler.class);
            handler.getClient().setProperty("state", state);
        }
    }

}

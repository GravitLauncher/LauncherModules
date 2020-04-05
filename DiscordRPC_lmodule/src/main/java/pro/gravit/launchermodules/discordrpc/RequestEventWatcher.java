package pro.gravit.launchermodules.discordrpc;

import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.websockets.ClientWebSocketService;

public class RequestEventWatcher implements ClientWebSocketService.EventHandler {
    public static RequestEventWatcher INSTANCE;
    public final boolean isClientInstance;

    public RequestEventWatcher(boolean isClientInstance) {
        this.isClientInstance = isClientInstance;
    }

    @Override
    public <T extends WebSocketEvent> boolean eventHandle(T event) {
        if (event instanceof AuthRequestEvent && ((AuthRequestEvent) event).playerProfile != null) {
            DiscordRPC.parameters.username = ((AuthRequestEvent) event).playerProfile.username;
            DiscordRPC.parameters.userUUID = ((AuthRequestEvent) event).playerProfile.uuid.toString();
            if (!isClientInstance) {
                Config c = new Config();
                boolean needUpdate = false;
                if (c.altAuthorizedFirstLine != null) {
                    DiscordRPC.presence.details = DiscordRPC.parameters.replace(c.altAuthorizedFirstLine);
                    needUpdate = true;
                }
                if (c.altAuthorizedSecondLine != null) {
                    DiscordRPC.presence.state = DiscordRPC.parameters.replace(c.altAuthorizedSecondLine);
                    needUpdate = true;
                }
                if (needUpdate) {
                    DiscordRPC.resetPresence();
                }
            }
        }
        return false;
    }
}

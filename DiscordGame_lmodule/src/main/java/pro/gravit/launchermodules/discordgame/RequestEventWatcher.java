package pro.gravit.launchermodules.discordgame;

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
            AuthRequestEvent event1 = (AuthRequestEvent) event;
            DiscordBridge.activityService.onLauncherAuth(event1.playerProfile);
        }
        return false;
    }
}

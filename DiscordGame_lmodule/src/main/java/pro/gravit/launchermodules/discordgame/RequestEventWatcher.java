package pro.gravit.launchermodules.discordgame;

import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.request.RequestService;
import pro.gravit.launcher.request.WebSocketEvent;

public class RequestEventWatcher implements RequestService.EventHandler {
    public static RequestEventWatcher INSTANCE;
    public final boolean isClientInstance;

    public RequestEventWatcher(boolean isClientInstance) {
        this.isClientInstance = isClientInstance;
    }

    @Override
    public <T extends WebSocketEvent> boolean eventHandle(T event) {
        if (event instanceof AuthRequestEvent event1 && ((AuthRequestEvent) event).playerProfile != null) {
            DiscordBridge.activityService.onLauncherAuth(event1.playerProfile);
        }
        return false;
    }
}

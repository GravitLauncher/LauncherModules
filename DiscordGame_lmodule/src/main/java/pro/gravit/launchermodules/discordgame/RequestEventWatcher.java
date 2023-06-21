package pro.gravit.launchermodules.discordgame;

import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.ExitRequestEvent;
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
        if (event instanceof AuthRequestEvent && ((AuthRequestEvent) event).playerProfile != null) {
            AuthRequestEvent authrequestevent = (AuthRequestEvent) event;
            DiscordBridge.activityService.updateAuthorizedStage(authrequestevent.playerProfile);
        }
        if (event instanceof ExitRequestEvent) {
            ExitRequestEvent exitEvent = (ExitRequestEvent) event;
            if (exitEvent.reason == ExitRequestEvent.ExitReason.NO_EXIT) {
                return false;
            }
            DiscordBridge.activityService.updateLoginStage();
        }
        return false;
    }
}

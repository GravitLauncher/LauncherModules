package pro.gravit.launchermodules.discordgame;

import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.ExitRequestEvent;
import pro.gravit.launcher.base.request.RequestService;
import pro.gravit.launcher.base.request.WebSocketEvent;

public class RequestEventWatcher implements RequestService.EventHandler {
    public static RequestEventWatcher INSTANCE;
    public final boolean isClientInstance;

    public RequestEventWatcher(boolean isClientInstance) {
        this.isClientInstance = isClientInstance;
    }

    @Override
    public <T extends WebSocketEvent> boolean eventHandle(T event) {
        if (event instanceof AuthRequestEvent authrequestevent && authrequestevent.playerProfile != null) {
            DiscordBridge.activityService.updateAuthorizedStage(authrequestevent.playerProfile);
        }
        if (event instanceof ExitRequestEvent exitEvent) {
            if (exitEvent.reason == ExitRequestEvent.ExitReason.NO_EXIT) {
                return false;
            }
            DiscordBridge.activityService.updateLoginStage();
        }
        return false;
    }
}

package pro.gravit.launchermodules.sentryl;

import pro.gravit.launcher.base.events.request.AuthRequestEvent;
import pro.gravit.launcher.base.events.request.ExitRequestEvent;
import pro.gravit.launcher.base.request.RequestService;
import pro.gravit.launcher.base.request.WebSocketEvent;

public class SentryEventHandler implements RequestService.EventHandler {
    @Override
    public <T extends WebSocketEvent> boolean eventHandle(T event) {
        if(event instanceof AuthRequestEvent authEvent) {
            if(authEvent.playerProfile == null) {
                return false;
            }
            SentryModule.currentHub.configureScope(scope -> scope.setUser(SentryModule.makeSentryUser(authEvent.playerProfile)));
        }
        if(event instanceof ExitRequestEvent exitEvent) {
            if(exitEvent.reason == ExitRequestEvent.ExitReason.NO_EXIT) {
                return false;
            }
            SentryModule.currentHub.configureScope(scope -> scope.setUser(null));
        }
        return false;
    }
}

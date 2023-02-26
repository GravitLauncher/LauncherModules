package pro.gravit.launchermodules.sentryl;

import io.sentry.Sentry;
import io.sentry.protocol.User;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.request.RequestService;
import pro.gravit.launcher.request.WebSocketEvent;

import java.util.HashMap;
import java.util.Map;

public class SentryEventHandler implements RequestService.EventHandler {
    @Override
    public <T extends WebSocketEvent> boolean eventHandle(T event) {
        if(event instanceof AuthRequestEvent) {
            AuthRequestEvent authEvent = (AuthRequestEvent) event;
            if(authEvent.playerProfile == null) {
                return false;
            }
            Sentry.configureScope(scope -> {
                scope.setUser(SentryModule.makeSentryUser(authEvent.playerProfile));
            });
        }
        return false;
    }
}

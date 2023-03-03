package pro.gravit.launchermodules.sentryl;

import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import pro.gravit.launchermodules.sentryl.utils.OshiUtils;

public class SentryEventProcessor implements EventProcessor {
    @Override
    public SentryEvent process(SentryEvent event, Hint hint) {
        if(SentryModule.config.collectMemoryInfo) {
            event.getContexts().put("Memory info", OshiUtils.makeMemoryProperties());
        }
        return event;
    }
}

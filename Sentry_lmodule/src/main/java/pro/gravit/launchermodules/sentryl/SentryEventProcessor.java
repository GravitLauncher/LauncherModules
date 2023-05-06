package pro.gravit.launchermodules.sentryl;

import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launchermodules.sentryl.utils.OshiUtils;
import pro.gravit.utils.helper.JVMHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SentryEventProcessor implements EventProcessor {
    @Override
    public SentryEvent process(SentryEvent event, Hint hint) {
        if(event.getThrowable() != null && SentryModule.config.ignoreErrors != null && SentryModule.config.ignoreErrors.contains(event.getThrowable().getMessage())) {
            return null;
        }
        if (event.getThrowable() instanceof RequestException) {
            event.setFingerprints(Collections.singletonList(event.getThrowable().getMessage()));
        }
        if(SentryModule.config.collectMemoryInfo) {
            event.getContexts().put("Memory info", OshiUtils.makeMemoryProperties());
        }
        long uptime = JVMHelper.RUNTIME_MXBEAN.getUptime();
        event.getContexts().put("Uptime", String.format("%ds %dms", uptime / 1000, uptime % 1000));
        return event;
    }
}

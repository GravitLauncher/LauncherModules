package pro.gravit.launchermodules.sentryl;

import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 1, 0, 1, Version.Type.LTS);

    public ModuleImpl() {
        super(new LauncherModuleInfo("SentryModule", version, new String[]{"ClientLauncherCore"}));
    }


    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, PreConfigPhase.class);
    }

    private void preInit(PreConfigPhase phase) {
        Config c = new Config();
        if ("YOUR_DSN".equals(c.dsn) || c.dsn == null) {
            LogHelper.error("Please, configure Sentry_lmodule config!!!");
            return;
        }
        try {
            Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Sentry.init(c.dsn);
            // this code will never throw anything :)
            LogHelper.addExcCallback(Sentry::capture);
            if (c.captureAll)
                LogHelper.addOutput(Sentry::capture, LogHelper.OutputTypes.PLAIN);
            if (c.setThreadExcpectionHandler)
                Thread.setDefaultUncaughtExceptionHandler(new CustomUncaughtExceptionHandler(defaultHandler));
        } catch (Throwable e) {
            LogHelper.error(e);
        }
    }
}

class CustomUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;

    CustomUncaughtExceptionHandler(Thread.UncaughtExceptionHandler defaultExceptionHandler) {
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable thrown) {
        if (thrown == null) return;
        EventBuilder eventBuilder = new EventBuilder()
                .withMessage(thrown.getMessage())
                .withLevel(Event.Level.FATAL)
                .withExtra("thread", thread != null ? thread.getName() : "ERR_nullThreadName")
                .withSentryInterface(new ExceptionInterface(thrown));

        try {
            Sentry.capture(eventBuilder);
        } catch (Exception e) {
            LogHelper.error(e);
        }

        // taken from ThreadGroup#uncaughtException
        if (defaultExceptionHandler != null) {
            // call the original handler
            defaultExceptionHandler.uncaughtException(thread, thrown);
        } else if (!(thrown instanceof ThreadDeath)) {
            System.err.print("Exception in thread \"" + thread.getName() + "\" ");
            thrown.printStackTrace(System.err);
        }
    }
}
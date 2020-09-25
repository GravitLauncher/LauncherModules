package pro.gravit.launchermodules.sentrys;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Files;
import java.nio.file.Path;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    private static final Gson GSON_P = new GsonBuilder().setPrettyPrinting().setLenient().create();
    public Config c = null;

    public ModuleImpl() {
        super(new LauncherModuleInfo("SentryServerModule", version, Integer.MAX_VALUE - 200, new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, LaunchServerInitPhase.class);
    }

    public void preInit(LaunchServerInitPhase phase) {
        try {
            Path p = phase.server.modulesManager.getConfigManager().getModuleConfig(this.moduleInfo.name);
            if (Files.isReadable(p)) {
                c = GSON_P.fromJson(IOHelper.decode(IOHelper.read(p)), Config.class);
            } else {
                Files.deleteIfExists(p);
                c = new Config();
                IOHelper.write(p, IOHelper.encode(GSON_P.toJson(c, Config.class)));
                LogHelper.error("Please configure Sentry_module config before use!");
                return; // First run. Bad config.
            }
            Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            if ("YOUR_DSN".equals(c.dsn) || c.dsn == null) {
                LogHelper.error("Please, configure Sentry_module config!!!");
                return;
            }
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

    public static class Config {
        public String dsn = "YOUR_DSN";
        public boolean captureAll = false;
        public boolean setThreadExcpectionHandler = false;
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

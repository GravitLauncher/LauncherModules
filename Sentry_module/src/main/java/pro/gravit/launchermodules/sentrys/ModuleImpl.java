package pro.gravit.launchermodules.sentrys;

import io.sentry.Sentry;
import io.sentry.log4j2.SentryAppender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.log4j.LogAppender;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.modules.events.LaunchServerNettyFullInitEvent;
import pro.gravit.utils.Version;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.JVMHelper;

import java.nio.file.Path;
import java.util.*;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    private static final String DEFAULT_DSN = "YOUR_DSN";
    private transient final Logger logger = LogManager.getLogger();
    public Config c = null;

    public SentryTransactionTracker tracker = new SentryTransactionTracker(this);
    private LaunchServer server;
    private SentryAppender appender;

    public ModuleImpl() {
        super(new LauncherModuleInfo("SentryServerModule", version, Integer.MAX_VALUE - 200, new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, LaunchServerInitPhase.class);
        registerEvent(this::onPostInit, LaunchServerNettyFullInitEvent.class);
    }

    public void preInit(LaunchServerInitPhase phase) {
        server = phase.server;
        try {
            Path p = modulesConfigManager.getModuleConfig(this.moduleInfo.name);
            JsonConfigurable<Config> configurable = new JsonConfigurable<>(Config.class, p) {
                @Override
                public Config getConfig() {
                    return c;
                }

                @Override
                public void setConfig(Config config) {
                    c = config;
                }

                @Override
                public Config getDefaultConfig() {
                    return new Config();
                }
            };
            configurable.loadConfig();
            if (c.dsn == null || c.dsn.equals(DEFAULT_DSN)) {
                logger.error("Please, configure Sentry_module config!!!");
                return;
            }
            Sentry.init(options -> {
                options.setDsn(c.dsn);
                options.setSampleRate(c.sampleRate);
                options.setEnableTracing(c.enableTracing);
                options.setTracesSampleRate(c.tracingSampleRate);
                options.setRelease(Version.getVersion().getVersionString());
                options.setEnvironment(Version.getVersion().release.name());
                options.setBeforeSend((event, hint) -> {
                    if (event.getThrowable() != null && c.ignoreErrors != null && c.ignoreErrors.contains(event.getThrowable().getMessage())) {
                        return null;
                    }
                    if (event.getThrowable() instanceof SentryTransactionTracker.RequestError) {
                        event.setFingerprints(List.of(event.getThrowable().toString()));
                    }
                    return event;
                });
            });
            Sentry.configureScope(scope -> {
                scope.setTag("java_version", String.valueOf(JVMHelper.RUNTIME_MXBEAN.getVmVersion()));
                scope.setContexts("modules", modulesList());
            });
            if (c.addSentryAppender) {
                appender = SentryAppender.createAppender("Sentry", Level.getLevel(c.appenderLogLevel), null, null, null, null, null);
                appender.start();
                LogAppender.getInstance().addListener(this::append);
            }
        } catch (Throwable e) {
            logger.error("Sentry module not configured", e);
        }
    }

    public void onPostInit(LaunchServerNettyFullInitEvent event) {
        if (c.requestTracker) {
            tracker.register(server.nettyServerSocketHandler);
        }
    }

    public void append(LogEvent event) {
        if (c.filterExceptions) {
            if (event.getThrownProxy() != null) {
                Throwable thrown = event.getThrownProxy().getThrowable();
                if (thrown instanceof CommandException) return;
            }
        }
        appender.append(event);
    }

    public Map<String, String> modulesList() {
        Map<String, String> map = new HashMap<>();
        for (LauncherModule module : server.modulesManager.getModules()) {
            LauncherModuleInfo info = module.getModuleInfo();
            map.put(info.name, info.version.getVersionString());
        }
        return map;
    }

    public static class Config {
        public String dsn = DEFAULT_DSN;
        public double sampleRate = 1.0;
        public boolean enableTracing = false;
        public double tracingSampleRate = 1.0;
        public boolean addSentryAppender = true;
        public boolean filterExceptions = true;
        public boolean requestTracker = true;
        public boolean captureRequestData = false;
        public boolean captureRequestError = false;
        public String appenderLogLevel = "ERROR";

        public List<String> ignoreErrors = new ArrayList<>(List.of("auth.wrongpassword"));

    }
}

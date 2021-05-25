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
import pro.gravit.utils.Version;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.JVMHelper;

import java.nio.file.Path;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    private static final String DEFAULT_DSN = "YOUR_DSN";
    private transient final Logger logger = LogManager.getLogger();
    public Config c = null;
    private LaunchServer server;
    private JsonConfigurable<Config> configurable;
    private SentryAppender appender;

    public ModuleImpl() {
        super(new LauncherModuleInfo("SentryServerModule", version, Integer.MAX_VALUE - 200, new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, LaunchServerInitPhase.class);
    }

    public void preInit(LaunchServerInitPhase phase) {
        server = phase.server;
        try {
            Path p = modulesConfigManager.getModuleConfig(this.moduleInfo.name);
            configurable = new JsonConfigurable<>(Config.class, p) {
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
            if (c.dsn == null) {
                logger.error("Please, configure Sentry_module config!!!");
                return;
            }
            if (c.dsn.equals(DEFAULT_DSN)) {
                logger.info("");
            }
            Sentry.init(options -> {
                options.setDsn(c.dsn);
                options.setRelease(Version.getVersion().getVersionString());
                options.setEnvironment(Version.getVersion().release.name());
            });
            Sentry.configureScope(scope -> {
                scope.setTag("java_version", String.valueOf(JVMHelper.RUNTIME_MXBEAN.getVmVersion()));
                scope.setContexts("modules", modulesList());
            });
            if (c.addSentryAppender) {
                appender = SentryAppender.createAppender("Sentry", null, Level.getLevel(c.appenderLogLevel), null, null);
                appender.start();
                LogAppender.getInstance().addListener(this::append);
            }
        } catch (Throwable e) {
            logger.error("Sentry module not configured", e);
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

    public String modulesList() {
        StringBuilder builder = new StringBuilder();
        for (LauncherModule module : server.modulesManager.getModules()) {
            LauncherModuleInfo info = module.getModuleInfo();
            builder.append(String.format("%s v%s | ", info.name, info.version.getVersionString()));
        }
        return builder.toString();
    }

    public static class Config {
        public String dsn = DEFAULT_DSN;
        public boolean addSentryAppender = true;
        public boolean filterExceptions = true;
        public String appenderLogLevel = "ERROR";
    }
}

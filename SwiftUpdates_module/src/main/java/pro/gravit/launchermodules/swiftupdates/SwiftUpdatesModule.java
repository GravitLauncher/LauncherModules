package pro.gravit.launchermodules.swiftupdates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.events.LaunchServerUpdatesSyncEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwiftUpdatesModule extends LauncherModule {
    private static final Logger logger = LogManager.getLogger(SwiftService.class);
    private SwiftService swiftService = null;
    private AtomicBoolean isEnabled = new AtomicBoolean(false);
    public SwiftService.Config config;

    public SwiftUpdatesModule() {
        super(new LauncherModuleInfo("SwiftUpdates", new Version(1, 0, 0), new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if (initContext instanceof LaunchServerInitContext context) {
            initS3UpdatesModule(context.server);
        } else {
            registerEvent(this::finish, LaunchServerFullInitEvent.class);
        }
        registerEvent(this::onUpdatePush, LaunchServerUpdatesSyncEvent.class);
    }

    public void initS3UpdatesModule(LaunchServer server) {
        final var path = modulesConfigManager.getModuleConfig(moduleInfo.name);
        SwiftUpdatesModule module = this;
        JsonConfigurable<SwiftService.Config> configurable = new JsonConfigurable<>(SwiftService.Config.class, path) {
            @Override
            public SwiftService.Config getConfig() {
                return config;
            }

            @Override
            public void setConfig(SwiftService.Config config) {
                module.config = config;
            }

            @Override
            public SwiftService.Config getDefaultConfig() {
                return new SwiftService.Config();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            config = configurable.getDefaultConfig();
        }
        config = configurable.getConfig();
        if (config.isEmpty()) {
            logger.error("SwiftUpdates module is not configured. Please configure it an restart LaunchServer it to enable.");
        } else {
            isEnabled.set(true);
        }
        if (isEnabled.get()) {
            swiftService = new SwiftService(config.openStackEndpoint,
                    config.openStackUsername,
                    config.openStackPassword,
                    config.openStackRegion,
                    config.openStackDomain);
        }
    }

    public void finish(LaunchServerFullInitEvent event) {
        initS3UpdatesModule(event.server);
    }

    private void onUpdatePush(LaunchServerUpdatesSyncEvent event) {
        if (isEnabled.get()) {
            try {
                swiftService.uploadDir(event.server.updatesDir, config.openStackContainer, config.prefix);
            } catch (IOException e) {
                logger.error("[SwiftUpdates] Error occurred while trying to fetch files for an update", e);
            }
        } else {
            logger.error("SwiftUpdates module is installed but not configured. No data will be pushed to Object Storage");
        }
    }

    public static String readableTime(long seconds) {
        return String.format("%02dm%02ds", (seconds % 3600) / 60, (seconds % 60));
    }

}

package pro.gravit.launchermodules.s3updates;

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

public class S3UpdatesModule extends LauncherModule {
    private static final Logger logger = LogManager.getLogger(S3Service.class);
    private S3Service s3Service = null;
    private final AtomicBoolean isEnabled = new AtomicBoolean(false);
    public S3Service.Config config;

    public S3UpdatesModule() {
        super(new LauncherModuleInfo("S3Updates", new Version(1, 0, 0), new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if (initContext instanceof LaunchServerInitContext context) {
            initSwiftUpdatesModule(context.server);
        } else {
            registerEvent(this::finish, LaunchServerFullInitEvent.class);
        }
        registerEvent(this::onUpdatePush, LaunchServerUpdatesSyncEvent.class);
    }

    public void initSwiftUpdatesModule(LaunchServer server) {
        final var path = modulesConfigManager.getModuleConfig(moduleInfo.name);
        S3UpdatesModule module = this;
        JsonConfigurable<S3Service.Config> configurable = new JsonConfigurable<>(S3Service.Config.class, path) {
            @Override
            public S3Service.Config getConfig() {
                return config;
            }

            @Override
            public void setConfig(S3Service.Config config) {
                module.config = config;
            }

            @Override
            public S3Service.Config getDefaultConfig() {
                return new S3Service.Config();
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
            logger.error("S3Updates module is not configured. Please configure it an restart LaunchServer to enable it");
        } else {
            isEnabled.set(true);
        }
        if (isEnabled.get()) {
            s3Service = new S3Service(config);
        }
        server.commandHandler.registerCommand("s3cleanup", new S3UpdatesCleanupCommand(server, s3Service, config));
        server.commandHandler.registerCommand("s3upload", new S3UpdatesCleanupCommand(server, s3Service, config));
    }

    public void finish(LaunchServerFullInitEvent event) {
        initSwiftUpdatesModule(event.server);
    }

    private void onUpdatePush(LaunchServerUpdatesSyncEvent event) {
        if (isEnabled.get()) {
            try {
                s3Service.uploadDir(event.server.updatesDir, config.s3Bucket, config.behavior.prefix, config.behavior.forceUpload, event.server.updatesManager);
            } catch (IOException e) {
                logger.error("[S3Updates] Error occurred while trying to fetch files for an update", e);
            }
        } else {
            logger.error("S3Updates module is not configured. No data will be pushed to Object Storage");
        }
    }

}

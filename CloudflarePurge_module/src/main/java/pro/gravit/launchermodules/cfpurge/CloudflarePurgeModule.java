package pro.gravit.launchermodules.cfpurge;

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

public class CloudflarePurgeModule extends LauncherModule {
    private static final Logger logger = LogManager.getLogger(CloudflareService.class);
    public CloudflareService.Config config;

    public CloudflarePurgeModule() {
        super(new LauncherModuleInfo("CloudflarePurgeModule", new Version(1, 0, 0), new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if (initContext instanceof LaunchServerInitContext context) {
            initCloudflarePurgeModule(context.server);
        } else {
            registerEvent(this::finish, LaunchServerFullInitEvent.class);
        }
        registerEvent(this::onUpdatePush, LaunchServerUpdatesSyncEvent.class);
    }

    public void initCloudflarePurgeModule(LaunchServer server) {
        final var path = modulesConfigManager.getModuleConfig(moduleInfo.name);
        CloudflarePurgeModule module = this;
        JsonConfigurable<CloudflareService.Config> configurable = new JsonConfigurable<>(CloudflareService.Config.class, path) {
            @Override
            public CloudflareService.Config getConfig() {
                return config;
            }

            @Override
            public void setConfig(CloudflareService.Config config) {
                module.config = config;
            }

            @Override
            public CloudflareService.Config getDefaultConfig() {
                return new CloudflareService.Config();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            config = configurable.getDefaultConfig();
        }
        config = configurable.getConfig();
        if ((config.cloudflareToken.isEmpty() || config.zoneIdentifier.isEmpty())) {
            logger.error("No or invalid Cloudflare token / zone id. Please fill out the config");
        }
    }

    public void finish(LaunchServerFullInitEvent event) {
        initCloudflarePurgeModule(event.server);
    }

    public void onUpdatePush(LaunchServerUpdatesSyncEvent event) {
        CloudflareService.INSTANCE.purgeAll(config.cloudflareToken, config.zoneIdentifier);
    }
}

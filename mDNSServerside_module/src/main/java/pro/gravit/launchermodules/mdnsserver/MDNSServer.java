package pro.gravit.launchermodules.mdnsserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launcher.base.modules.events.ClosePhase;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.utils.Version;

import java.io.IOException;

public class MDNSServer extends LauncherModule {
    public static final Version version = new Version(1, 0, 2, 1, Version.Type.STABLE);
    private static boolean registred = false;
    public JsonConfigurable<MDNSConfig> configurable;
    public MDNSConfig config;
    public MDNSService mdnsService;
    private final Logger logger = LogManager.getLogger();

    public MDNSServer() {
        super(new LauncherModuleInfoBuilder().setName("mDNSServer").setVersion(version).setDependencies(new String[]{"LaunchServerCore"}).createLauncherModuleInfo());
        var module = this;
        configurable = new JsonConfigurable<>(MDNSConfig.class, modulesConfigManager.getModuleConfig(moduleInfo.name)) {
            @Override
            public MDNSConfig getConfig() {
                return config;
            }

            @Override
            public void setConfig(MDNSConfig config) {
                module.config = config;
            }

            @Override
            public MDNSConfig getDefaultConfig() {
                return new MDNSConfig();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            config = configurable.getDefaultConfig();
        }
    }

    public void start(LaunchServerFullInitEvent event) {
        try {
            mdnsService = new MDNSService(config.serviceType, config.serviceName, config.servicePort, "");
        } catch (Exception e) {
            logger.error("Failed to setup mDNS server", e);
        }
    }

    public void close(ClosePhase closePhase) {
        if(mdnsService != null) {
            try {
                mdnsService.close();
            } catch (Exception e) {
                logger.error("Failed to close mDNS server", e);
            }
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::start, LaunchServerFullInitEvent.class);
        registerEvent(this::close, ClosePhase.class);
    }
}

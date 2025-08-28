package pro.gravit.launcher.launchermodules.prestarter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.launchserver.modules.events.LaunchServerLauncherBinaryInit;
import pro.gravit.utils.Version;

import java.io.IOException;

public class PrestarterModule extends LauncherModule {

    private final Logger logger = LogManager.getLogger();
    public Config config;
    public JsonConfigurable<Config> configurable;

    public PrestarterModule() {
        super(new LauncherModuleInfo("Prestarter", Version.of(1,0,0), new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::init, LaunchServerLauncherBinaryInit.class);
    }

    public void init(LaunchServerLauncherBinaryInit init) {
        for(var path : config.paths.entrySet()) {
            init.binary.put(path.getKey(), new PrestarterLauncherBinary(init.server, this, path.getKey()));
        }
        PrestarterModule module = this;
        configurable = new JsonConfigurable<>(Config.class, modulesConfigManager.getModuleConfig(moduleInfo.name)) {
            @Override
            public Config getConfig() {
                return config;
            }

            @Override
            public void setConfig(Config config) {
                module.config = config;
            }

            @Override
            public Config getDefaultConfig() {
                return new Config();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            config = configurable.getDefaultConfig();
        }
    }

}

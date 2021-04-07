package pro.gravit.launchermodules.osslsigncode;

import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.exe.Launch4JTask;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.Path;

public class OSSLSignCodeModule extends LauncherModule {
    public OSSLSignCodeConfig config;

    public OSSLSignCodeModule() {
        super(new LauncherModuleInfo("OSSLSignCode", new Version(1, 0, 0), new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if (initContext instanceof LaunchServerInitContext) {
            initOSSLSignCode(((LaunchServerInitContext) initContext).server);
        } else {
            registerEvent(this::registerTask, LaunchServerFullInitEvent.class);
        }
    }

    public void registerTask(LaunchServerFullInitEvent event) {
        initOSSLSignCode(event.server);
    }

    public void initOSSLSignCode(LaunchServer server) {
        Path configPath = modulesConfigManager.getModuleConfig(moduleInfo.name);
        OSSLSignCodeModule module = this;
        JsonConfigurable<OSSLSignCodeConfig> configurable = new JsonConfigurable<>(OSSLSignCodeConfig.class, configPath) {
            @Override
            public OSSLSignCodeConfig getConfig() {
                return config;
            }

            @Override
            public void setConfig(OSSLSignCodeConfig config) {
                module.config = config;
            }

            @Override
            public OSSLSignCodeConfig getDefaultConfig() {
                return OSSLSignCodeConfig.getDefault();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            LogHelper.error(e);
            return;
        }
        config = configurable.getConfig();
        server.commandHandler.registerCommand("osslsignexe", new OSSLSignEXECommand(server, config));
        server.launcherEXEBinary.addAfter((t) -> t instanceof Launch4JTask, new OSSLSignTask(server, config));
    }
}

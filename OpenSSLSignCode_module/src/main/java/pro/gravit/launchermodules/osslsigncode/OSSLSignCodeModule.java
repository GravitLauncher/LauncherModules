package pro.gravit.launchermodules.osslsigncode;

import pro.gravit.launcher.config.SimpleConfigurable;
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

public class OSSLSignCodeModule extends LauncherModule {
    public OSSLSignCodeModule() {
        super(new LauncherModuleInfo("OSSLSignCode", new Version(1, 0, 0), new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if (initContext instanceof LaunchServerInitContext) {
            registerTask(new LaunchServerFullInitEvent(((LaunchServerInitContext) initContext).server));
        } else {
            registerEvent(this::registerTask, LaunchServerFullInitEvent.class);
        }
    }

    public void registerTask(LaunchServerFullInitEvent event) {
        OSSLSignCodeConfig config;
        SimpleConfigurable<OSSLSignCodeConfig> configurable = modulesConfigManager.getConfigurable(OSSLSignCodeConfig.class, moduleInfo.name);
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            LogHelper.error(e);
            return;
        }
        config = configurable.getConfig();
        LaunchServer server = event.server;
        server.commandHandler.registerCommand("osslsignexe", new OSSLSignEXECommand(server, config));
        server.launcherEXEBinary.addAfter((t) -> t instanceof Launch4JTask, new OSSLSignTask(server, config));
    }
}

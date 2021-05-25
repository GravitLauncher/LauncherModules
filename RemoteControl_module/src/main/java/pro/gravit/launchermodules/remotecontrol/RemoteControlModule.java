package pro.gravit.launchermodules.remotecontrol;

import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchermodules.remotecontrol.commands.RemoteControlCommand;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class RemoteControlModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    public RemoteControlConfig config;
    public JsonConfigurable<RemoteControlConfig> configurable;

    public RemoteControlModule() {
        super(new LauncherModuleInfo("RemoteControl", version, new String[]{"LaunchServerCore"}));
    }


    public void finish(LaunchServerFullInitEvent event) {
        initRemoteControl(event.server);
    }

    public void initRemoteControl(LaunchServer server) {
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            LogHelper.error(e);
            config = configurable.getDefaultConfig();
        }
        server.commandHandler.registerCommand("remotecontrol", new RemoteControlCommand(server));
        NettyWebAPIHandler.addNewSeverlet("remotecontrol/command", new RemoteControlWebSeverlet(this, server));
        if (config.enabled) {
            LogHelper.info("RemoteControl enabled. Found %d access tokens", config.list.size());
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        Path path = modulesConfigManager.getModuleConfig(moduleInfo.name);
        RemoteControlModule module = this;
        configurable = new JsonConfigurable<>(RemoteControlConfig.class, path) {
            @Override
            public RemoteControlConfig getConfig() {
                return module.config;
            }

            @Override
            public void setConfig(RemoteControlConfig config) {
                module.config = config;
            }

            @Override
            public RemoteControlConfig getDefaultConfig() {
                RemoteControlConfig config1 = new RemoteControlConfig();
                config1.list = new ArrayList<>();
                config1.enabled = true;
                config1.list.add(new RemoteControlConfig.RemoteControlToken(SecurityHelper.randomStringToken(), 0, true, new String[0]));
                return config1;
            }
        };
        if (initContext instanceof LaunchServerInitContext) {
            initRemoteControl(((LaunchServerInitContext) initContext).server);
            return;
        }
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
    }
}

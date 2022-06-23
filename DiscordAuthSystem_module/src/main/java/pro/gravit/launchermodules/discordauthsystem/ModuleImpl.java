package pro.gravit.launchermodules.discordauthsystem;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordApi;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordSystemAuthCoreProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 0, Version.Type.LTS);
    private static boolean registred = false;
    private final transient Logger logger = LogManager.getLogger();
    public JsonConfigurable<Config> jsonConfigurable;
    public Config config;

    public ModuleImpl() {
        super(new LauncherModuleInfo("DiscordAuthSystem", version, new String[]{"LaunchServerCore"}));
    }

    public void preConfig(PreConfigPhase preConfigPhase) {
        if (!registred) {
            AuthCoreProvider.providers.register("discordauthsystem", DiscordSystemAuthCoreProvider.class);
            registred = true;
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preConfig, PreConfigPhase.class);
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
        jsonConfigurable = modulesConfigManager.getConfigurable(Config.class, moduleInfo.name);
    }

    public void finish(LaunchServerFullInitEvent event) {
        LaunchServer launchServer = event.server;
        try {
            jsonConfigurable.loadConfig();
            config = jsonConfigurable.getConfig();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        DiscordApi.initialize(config);
        NettyWebAPIHandler.addNewSeverlet("auth/discord", new WebApi(this, launchServer));
    }
}
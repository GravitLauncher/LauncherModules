package pro.gravit.launchermodules.telegrambot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

import java.io.IOException;
import java.nio.file.Path;

public class TelegramBotModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.STABLE);
    private static final Logger logger = LogManager.getLogger(TelegramBot.class);
    public TelegramBot.Config config;
    public JsonConfigurable<TelegramBot.Config> configurable;


    public TelegramBotModule() {
        super(new LauncherModuleInfo("TelegramBot", version, new String[]{"LaunchServerCore"}));
    }


    public void finish(LaunchServerFullInitEvent event) {
        init(event.server);
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
        if (initContext instanceof LaunchServerInitContext launchServerInitContext) {
            init(launchServerInitContext.server);
        }
    }

    public void init(LaunchServer server) {
        Path path = modulesConfigManager.getModuleConfig(moduleInfo.name);
        TelegramBotModule module = this;
        configurable = new JsonConfigurable<>(TelegramBot.Config.class, path) {
            @Override
            public TelegramBot.Config getConfig() {
                return config;
            }

            @Override
            public void setConfig(TelegramBot.Config config) {
                module.config = config;
            }

            @Override
            public TelegramBot.Config getDefaultConfig() {
                return new TelegramBot.Config();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            config = configurable.getDefaultConfig();
        }
        TelegramBot.initialize(config, server);
        if (config.events.login) {
            server.authHookManager.postHook.registerHook((context, client) -> {
                TelegramBotListener.getInstance()
                        .sendNotify("""
                                        Пользователь %s авторизовался в лаунчере.
                                        *UUID:* %s
                                        *AuthId:* %s"""
                                .formatted(client.username, client.uuid.toString(), client.auth.displayName));
                return false;
            });
        }
        if (config.events.checkServer) {
            server.authHookManager.postCheckServerHook.registerHook((report, client) -> {
                String serverName = client.getProperty("launchserver.serverName");
                if (serverName == null) {
                    serverName = "Unknown";
                }
                TelegramBotListener.getInstance()
                        .sendNotify("""
                                        Пользователь %s входит на сервер %s
                                        *UUID:* %s"""
                                .formatted(report.playerProfile != null ? report.playerProfile.username : report.user.getUsername(),
                                        serverName, report.uuid.toString()));
                return false;
            });
        }
    }
}

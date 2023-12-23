package pro.gravit.launchermodules.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
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

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Path;

public class DiscordBotModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.STABLE);
    private static final Logger logger = LogManager.getLogger(DiscordBot.class);
    public DiscordBot.Config config;
    public JsonConfigurable<DiscordBot.Config> configurable;


    public DiscordBotModule() {
        super(new LauncherModuleInfo("DiscordBot", version, new String[]{"LaunchServerCore"}));
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
        DiscordBotModule module = this;
        configurable = new JsonConfigurable<>(DiscordBot.Config.class, path) {
            @Override
            public DiscordBot.Config getConfig() {
                return config;
            }

            @Override
            public void setConfig(DiscordBot.Config config) {
                module.config = config;
            }

            @Override
            public DiscordBot.Config getDefaultConfig() {
                return new DiscordBot.Config();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            config = configurable.getDefaultConfig();
        }
        try {
            DiscordBot.initialize(config, server);
        } catch (LoginException e) {
            logger.error("DiscordBotModule disabled. Please set 'token'", e);
        }
        if (config.events.login) {
            server.authHookManager.postHook.registerHook((context, client) -> {
                DiscordBot.sendEvent(new MessageBuilder()
                        .append("Пользователь %s авторизовался в лаунчере".formatted(client.username))
                        .setEmbeds(new EmbedBuilder()
                                .addField("UUID", client.uuid.toString(), false)
                                .addField("AuthId", client.auth.displayName, false)
                                .build())
                        .build());
                return false;
            });
        }
        if (config.events.checkServer) {
            server.authHookManager.postCheckServerHook.registerHook((report, client) -> {
                String serverName = client.getProperty("launchserver.serverName");
                if (serverName == null) {
                    serverName = "Unknown";
                }
                DiscordBot.sendEvent(new MessageBuilder()
                        .append("Пользователь %s входит на сервер %s".formatted(report.playerProfile != null ? report.playerProfile.username : report.user.getUsername(), serverName))
                        .setEmbeds(new EmbedBuilder()
                                .addField("UUID", report.uuid.toString(), false)
                                .build())
                        .build());
                return false;
            });
        }
    }
}

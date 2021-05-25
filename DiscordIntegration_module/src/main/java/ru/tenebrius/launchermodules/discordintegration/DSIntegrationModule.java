package ru.tenebrius.launchermodules.discordintegration;

import net.dv8tion.jda.api.JDABuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.config.SimpleConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.Version;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class DSIntegrationModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 1, 1, Version.Type.BETA);
    private static transient final Logger logger = LogManager.getLogger();
    public static Config config = null;
    private static boolean registred = false;
    private static String log = "";

    public DSIntegrationModule() {
        super(new LauncherModuleInfo("DiscordIntegration", version, new String[]{"LaunchServerCore"}));
    }

    public static void sendMsg(String msg) {
        log += msg + "\n";
        send(false);
    }

    public static void send(boolean force) {
        if (log.length() > 1700 || force) {
            if (log.equals("")) return;
            DiscordWebhook webhook = new DiscordWebhook(config.webhook);
            webhook.setUsername("Log");
            if (log.length() > 1900)
                log = log.substring(0, 1900) + "...";
            webhook.setContent("```" + log + "```");
            try {
                webhook.execute();
            } catch (IOException e) {
                logger.error(e);
            }
            log = "";
        }
    }

    public void preInit(PreConfigPhase preConfigPhase) {
        if (!registred) {
            registred = true;
        }
    }

    public boolean authHook(AuthResponse.AuthContext context, Client client) {
        DiscordWebhook webhook = new DiscordWebhook(config.webhook);
        if (!config.logAuth)
            return false;
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject()
                .setTitle("Player join!")
                .setDescription("Username: " + context.login)
                .setColor(new Color(ThreadLocalRandom.current().nextInt(0, 0xFFFFFF)));
        if (config.avatarEnable)
            embedObject.setThumbnail(String.format(config.url, context.login));
        webhook.addEmbed(embedObject);
        try {
            webhook.execute();
        } catch (IOException e) {
            logger.error(e);
        }
        return false;
    }

    public void initConfig(LaunchServerInitPhase phase) {
        phase.server.authHookManager.postHook.registerHook(this::authHook);
        SimpleConfigurable<Config> configurable = modulesConfigManager.getConfigurable(Config.class, moduleInfo.name);
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            return;
        }
        config = configurable.getConfig();
    }

    public void postInit(LaunchServerFullInitEvent event) {
        DiscordWebhook webhook = new DiscordWebhook(config.webhook);
        DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject()
                .setTitle("Launcher started!")
                .setDescription("Launcher started!")
                .setColor(new Color(ThreadLocalRandom.current().nextInt(0, 0xFF) << 8));
        /*StringBuilder profiles = new StringBuilder();
        if (event.server.getProfiles().size() == 0) {
            profiles.append("Profiles not found");
        } else {
            for (ClientProfile profile : event.server.getProfiles()) {
                profiles.append(" - ").append(profile.getTitle()).append("\n");
            }
        }
        embedObject.addField("Profiles:", profiles.toString(), true);*/
        webhook.addEmbed(embedObject);

        if (config.bot) {
            JDABuilder builder = JDABuilder.createDefault(config.token);
            builder.addEventListeners(new MessageListener(event.server));
            try {
                builder.build();
            } catch (LoginException e) {
                logger.error(e);
            }
        }
        try {
            webhook.execute();
        } catch (IOException e) {
            logger.error(e);
        }

    }


    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, PreConfigPhase.class);
        registerEvent(this::initConfig, LaunchServerInitPhase.class);
        registerEvent(this::postInit, LaunchServerFullInitEvent.class);
    }

    public static class Config {
        boolean logAuth = true;
        boolean avatarEnable = true;
        String prefix = "!";
        String url = "https://minotar.net/cube/user/%s.png";
        String webhook = "https://discord.com/api/webhooks/{YOUR_WEBHOOK}";

        boolean bot = true;
        String token = "MY_TOKEN";
        String channelID = "CHANNEL_ID";
        boolean adminOnly = true;
    }
}

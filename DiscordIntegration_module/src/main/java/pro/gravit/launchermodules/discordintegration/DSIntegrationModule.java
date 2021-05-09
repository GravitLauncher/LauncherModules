package pro.gravit.launchermodules.discordintegration;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.config.SimpleConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.Version;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;
import java.util.Date;

public class DSIntegrationModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 1, 1, Version.Type.BETA);
    private static transient final Logger logger = LogManager.getLogger();
    public static Config config = null;
    private static boolean registred = false;
    private static String log = "";
    private static JDA api;


    public DSIntegrationModule() {
        super(new LauncherModuleInfo("DiscordIntegration", version, new String[]{"LaunchServerCore"}));
    }

    public static void sendMsg(String msg) {
        log += msg + "\n";
        send(false);
    }

    public static void send(boolean force) {
        log = log.replaceAll("(\\d{4}.\\d{2}.\\d{2} \\d{2}:\\d{2}:\\d{2} )", "");
        if (log.length() > 1700 || force) {
            if (log.equals("")) return;
            if (log.length() > 1900)
                log = log.substring(0, 1900) + "...";
            discordMsg(null, null, "```ps\n" + log + "\n```", -1, null);
            log = "";
        }
    }

    public static Color getColor(Integer color) {
        switch (color) {
            case 0:
                return new Color(0x7289DA); // DISCORD
            case 1:
                return new Color(0x43B581); // ONLINE
            case 2:
                return new Color(0xF04747); // DND
            case 3:
                return new Color(0xFAA61A); // IDLE
            case 4:
                return new Color(0xFF73FA); // NITRO
            case 5:
                return new Color(0x9B84EE); // BRAVERY
            case 6:
                return new Color(0x44DDBF); // BALANCE
            case 7:
                return new Color(0xF47B68); // BRILLIANCE
            case 8:
                return new Color(0xF57731); // HIGH
            case 9:
                return new Color(0xF9C9A9); // SKIN
            case 10:
                return new Color(0x99AAB5); // GREY
            default:
                return new Color(0xFFFFFF); // WHITE
        }
    }

    public static void discordMsg(String author, String title, String description, Integer color, String thumbnail) {
        TextChannel channel = api.getTextChannelById(DSIntegrationModule.config.channelID);
        if (channel != null) {
            if (color < 0) {
                channel.sendMessage(description).queue();
            } else {
                Color cl = getColor(color);
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle(title)
                        .setColor(cl)
                        .setTimestamp(new Date().toInstant());
                if (author != null) eb.setAuthor(author);
                if (description != null) eb.setDescription(description);
                if (thumbnail != null) eb.setThumbnail(thumbnail);
                channel.sendMessage(eb.build()).queue();
            }
        }
    }

    public void preInit(PreConfigPhase preConfigPhase) {
        if (!registred) {
            registred = true;
        }
    }

    public boolean authHook(AuthResponse.AuthContext context, Client client) {
        if (!config.logAuth)
            return false;
        String thumbnail = null;
        if (config.avatarEnable) thumbnail = String.format(config.url, context.login);
        discordMsg("Авторизирован", context.login, null, config.colorAuth, thumbnail);
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
        if (config.bot) {
            JDABuilder builder = JDABuilder.createDefault(config.token);
            builder.addEventListeners(new MessageListener(event.server));
            try {
                JDA jda = builder.build();

                jda.awaitReady();
                api = jda;
            } catch (LoginException | InterruptedException e) {
                logger.error(e);
            }
            String pr = null;
            if (config.logProfiles) {
                StringBuilder profiles = new StringBuilder();
                if (event.server.getProfiles().size() == 0) {
                    profiles.append("Профили не найдены");
                } else {
                    profiles.append("**Список профилей:**\n\n");
                    for (ClientProfile profile : event.server.getProfiles()) {
                        profiles.append(" - **" + profile.getTitle() + " " + profile.getVersion().toString().replace("Minecraft ", "") + "**\n");
                        profiles.append("sortIndex: " + profile.getSortIndex() + "\n");
                        profiles.append("UUID: " + profile.getUUID() + "\n\n");
                    }
                }
                pr = profiles.toString();
            }
            discordMsg(null, "Лаунчер Запущен", pr, config.colorRun, null);
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
        boolean logProfiles = true;
        boolean avatarEnable = true;
        String prefix = "!";
        String url = "https://minotar.net/cube/user/%s.png";
        boolean bot = true;
        String token = "MY_TOKEN";
        String channelID = "CHANNEL_ID";
        boolean adminOnly = true;
        Integer colorRun = 1;
        Integer colorAuth = 8;

    }

}

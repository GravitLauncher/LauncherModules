package pro.gravit.launchermodules.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

public class DiscordBot {
    private static final Logger logger = LogManager.getLogger(DiscordBot.class);
    private static JDA jda;
    private static Config config;

    public static JDA getJda() {
        return jda;
    }

    public static Config getConfig() {
        return config;
    }

    public static void initialize(Config config, LaunchServer server) throws LoginException {
        DiscordBot.config = config;
        jda = JDABuilder.createDefault(config.token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(new DiscordBotListener(config, server))
                .build();
    }

    public static void sendEvent(Message message) {
        if (jda == null) {
            return;
        }
        TextChannel channel = jda.getTextChannelById(config.eventChannelId);
        if (channel == null) {
            logger.error("Channel {} not found", config.eventChannelId);
            return;
        }
        channel.sendMessage(message).queue();
    }

    public static class Config {
        public String token = "";
        public String prefix = "!";
        public String color = "";
        public boolean avatarEnable = false;
        public String avatar_url = "https://minotar.net/cube/user/%s.png";
        public long eventChannelId = 12345;
        public List<String> allowUsers = new ArrayList<>();
        public List<String> allowRoles = new ArrayList<>();
        public EventsConfig events = new EventsConfig();
    }

    public static class EventsConfig {
        public boolean login;
        public boolean selectProfile;
        public boolean checkServer;
    }
}

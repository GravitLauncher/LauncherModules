package pro.gravit.launchermodules.telegrambot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import pro.gravit.launchserver.LaunchServer;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;

public class TelegramBot {
    private static final Logger logger = LogManager.getLogger(TelegramBot.class);
    private static Config config;

    public static Config getConfig() {
        return config;
    }

    public static void initialize(Config config, LaunchServer server) throws LoginException {
        TelegramBot.config = config;
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(TelegramBotListener.getInstance(config, server));
            logger.info("TelegramAPI started. Look for messages");
        } catch (TelegramApiException e) {
            logger.error("Cant Connect. " + e.getMessage());
        }

    }

    public static class Config {
        public String token = "";
        public String botUsername = "";
        public String prefix = "!";
        public String channelID = "";
        public List<String> allowUsers = new ArrayList<>();
        public EventsConfig events = new EventsConfig();
        public boolean logging = true;
    }

    public static class EventsConfig {
        public boolean login;
        public boolean checkServer;
    }
}

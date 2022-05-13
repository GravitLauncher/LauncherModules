package pro.gravit.launchermodules.telegrambot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.log4j.LogAppender;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.CommonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TelegramBotListener extends TelegramLongPollingBot {
    private static TelegramBotListener instance;
    private final TelegramBot.Config config;
    private final LaunchServer server;
    private final Logger logger = LogManager.getLogger(TelegramBotListener.class);

    public static TelegramBotListener getInstance(TelegramBot.Config config, LaunchServer server) {
        if (instance == null) {
            instance = new TelegramBotListener(config, server);
        }
        return instance;
    }

    public static TelegramBotListener getInstance() {
        return instance;
    }

    public TelegramBotListener(TelegramBot.Config config, LaunchServer server) {
        this.config = config;
        this.server = server;
    }

    @Override
    public String getBotToken() {
        return config.token;
    }


    @Override
    public String getBotUsername() {
        return config.botUsername;
    }

    public boolean check(User user) {
        return config.allowUsers != null && (config.allowUsers.contains(user.getUserName()) ||
                config.allowUsers.contains(Long.toString(user.getId())));
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMyChatMember() && update.getMyChatMember().getChat() != null) {
            SendMessage message = new SendMessage();
            message.enableMarkdown(true);
            message.setChatId(String.valueOf(update.getMyChatMember().getChat().getId()));
            message.setText("channelID: " + update.getMyChatMember().getChat().getId());
            send(message);
        } else if (update.hasMessage() && update.getMessage().isUserMessage()) {
            if (config.logging)
                logger.debug("TelegramBot << ({}, id:{}) message: {}",
                        update.getMessage().getFrom().getUserName(),
                        update.getMessage().getFrom().getId(),
                        update.getMessage().getText());
            messageHandler(update);
        }
    }

    private void messageHandler(Update update) {
        String content = update.getMessage().getText();
        User user = update.getMessage().getFrom();
        long chatId = update.getMessage().getChatId();

        if (content.startsWith(config.prefix)) {
            SendMessage message = new SendMessage();
            message.enableMarkdown(true);
            message.setChatId(String.valueOf(chatId));
            if (!check(user)) {
                message.setText("У вас недостаточно прав для выполнения команд");
                send(message);
                return;
            }
            LogLinesContainer container = new LogLinesContainer();
            LogAppender.getInstance().addListener(container);
            try {
                String[] cmd = CommonHelper.parseCommand(content.substring(config.prefix.length()));
                Command command = server.commandHandler.findCommand(cmd[0]);
                if (command == null) {
                    throw new CommandException(String.format("Command '%s' not found", cmd[0]));
                }
                String[] args = new String[cmd.length - 1];
                System.arraycopy(cmd, 1, args, 0, cmd.length - 1);
                command.invoke(args);
                String fullLog = container.lines.stream().map((x) -> String.format("[%s] %s %s", x.level, x.message, x.exception == null ? "" : x.exception)).collect(Collectors.joining("\n"));

                message.setText("Команда выполнена успешно. Лог:\n```" + fullLog + "```");
                send(message);
            } catch (CommandException e) {
                message.setText("Произошла ошибка при парсинге команды: ".concat(e.getMessage()));
                send(message);
            } catch (Exception e) {
                String fullLog = container.lines.stream().map((x) -> String.format("[%s] %s %s", x.level, x.message, x.exception)).collect(Collectors.joining("\n"));
                message.setText("Произошла ошибка при выполнении команды: \n```" + e + "```\n" +
                        "```" + fullLog + "```");
                send(message);
            } finally {
                LogAppender.getInstance().removeListener(container);
            }
        }
    }

    public void sendNotify(String text) {
        if (config.channelID.isEmpty())
            return;
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        message.setChatId(config.channelID);
        message.setText(text);
        send(message);
    }

    public void send(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static class LogLinesContainer implements Consumer<LogEvent> {
        public List<LogEventView> lines = new ArrayList<>();
        private final Thread currentThread = Thread.currentThread();

        @Override
        public void accept(LogEvent logEvent) {
            if (Thread.currentThread() == currentThread) {
                lines.add(new LogEventView(logEvent));
            }
        }
    }

    public static class LogEventView {
        public final String level;
        public final String message;
        public final String exception;

        public LogEventView(LogEvent event) {
            level = event.getLevel().toString();
            message = event.getMessage().getFormattedMessage().replaceAll("\u001B\\[[;\\d]*m", "");
            Throwable throwable = event.getMessage().getThrowable();
            if (throwable != null) {
                exception = String.format("%s: %s", throwable.getClass().getName(), throwable.getMessage());
            } else {
                exception = null;
            }
        }
    }
}

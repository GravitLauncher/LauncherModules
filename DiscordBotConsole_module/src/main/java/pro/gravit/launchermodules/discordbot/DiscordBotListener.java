package pro.gravit.launchermodules.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.jetbrains.annotations.NotNull;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.log4j.LogAppender;
import pro.gravit.utils.Version;
import pro.gravit.utils.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.utils.helper.CommonHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DiscordBotListener extends ListenerAdapter {
    private final DiscordBot.Config config;
    private final LaunchServer server;
    private final Logger logger = LogManager.getLogger(DiscordBotListener.class);

    public DiscordBotListener(DiscordBot.Config config, LaunchServer server) {
        this.config = config;
        this.server = server;
    }

    public boolean check(User user, Member member) {
        if(config.allowUsers != null && config.allowUsers.contains(user.getId())) {
            return true;
        }
        if(config.allowRoles != null && member != null) {
            for(var e : member.getRoles()) {
                if(config.allowRoles.contains(e.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        EmbedBuilder embedStarted = new EmbedBuilder()
                .setTitle(String.format("Лаунчер v%s успешно запущен!", Version.getVersion()));

        if (config.color.isEmpty()) {
            embedStarted.setColor(new Color(ThreadLocalRandom.current().nextInt(0, 0xFFFFFF)));
        } else if (config.color.startsWith("#")) {
            embedStarted.setColor(Color.decode(config.color));
        }

        StringBuilder profiles = new StringBuilder();
        if (this.server.getProfiles().size() == 0) {
            profiles.append("Профили не найдены");
        } else {
            for (ClientProfile profile : this.server.getProfiles()) {
                profiles.append(" - ").append(profile.getTitle()).append("\n");
            }
        }
        embedStarted.addField("Профили:", profiles.toString(), true);

        DiscordBot.sendEvent(new MessageBuilder().setEmbeds(embedStarted.build()).build());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        User user = event.getAuthor();
        Member member = event.getMember();
        MessageChannel channel = event.getChannel();
        String content = message.getContentRaw();
        if(content.startsWith(config.prefix)) {
            if(!check(user, member)) {
                channel.sendMessage(new MessageBuilder()
                        .append("У вас недостаточно прав для выполнения команд")
                        .build()).queue();
                return;
            }
            LogLinesContainer container = new LogLinesContainer();
            LogAppender.getInstance().addListener(container);
            try {
                String[] cmd = CommonHelper.parseCommand(content.substring(config.prefix.length()));
                Command command = server.commandHandler.findCommand(cmd[0]);
                if(command == null) {
                    throw new CommandException(String.format("Command '%s' not found", cmd[0]));
                }
                String[] args = new String[cmd.length-1];
                System.arraycopy(cmd, 1, args, 0, cmd.length-1);
                command.invoke(args);
                String fullLog = container.lines.stream().map((x) -> String.format("[%s] %s %s", x.level, x.message, x.exception == null ? "" : x.exception)).collect(Collectors.joining("\n"));
                channel.sendMessage(new MessageBuilder()
                        .append("Команда выполнена успешно. Лог: ")
                        .appendCodeBlock(fullLog, "")
                        .build()).queue();
            } catch (CommandException e) {
                channel.sendMessage(new MessageBuilder()
                        .append("Произошла ошибка при парсинге команды: ".concat(e.getMessage()))
                        .build()).queue();
            } catch (Exception e) {
                String fullLog = container.lines.stream().map((x) -> String.format("[%s] %s %s", x.level, x.message, x.exception)).collect(Collectors.joining("\n"));
                channel.sendMessage(new MessageBuilder()
                        .append("Произошла ошибка при выполнении команды: ")
                        .appendCodeBlock(e.toString(), "java")
                        .append("Лог: ")
                        .appendCodeBlock(fullLog, "")
                        .build()).queue();
            } finally {
                LogAppender.getInstance().removeListener(container);
            }
        }
    }

    public static class LogLinesContainer implements Consumer<LogEvent> {
        public List<LogEventView> lines = new ArrayList<>();
        private final Thread currentThread = Thread.currentThread();

        @Override
        public void accept(LogEvent logEvent) {
            if(Thread.currentThread() == currentThread) {
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

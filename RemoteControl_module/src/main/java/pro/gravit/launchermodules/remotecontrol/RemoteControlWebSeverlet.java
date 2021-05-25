package pro.gravit.launchermodules.remotecontrol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.config.log4j.LogAppender;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RemoteControlWebSeverlet implements NettyWebAPIHandler.SimpleSeverletHandler {
    private final RemoteControlModule module;
    private final LaunchServer server;
    private transient final Logger logger = LogManager.getLogger();

    public RemoteControlWebSeverlet(RemoteControlModule module, LaunchServer server) {
        this.module = module;
        this.server = server;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, FullHttpRequest msg, NettyConnectContext context) throws Exception {
        if (!module.config.enabled) {
            sendHttpResponse(ctx, simpleJsonResponse(HttpResponseStatus.FORBIDDEN, new RemoteControlResponse<Void>("RemoteControl disabled")));
            return;
        }
        if (msg.method() != HttpMethod.GET && msg.method() != HttpMethod.POST) {
            sendHttpResponse(ctx, simpleJsonResponse(HttpResponseStatus.METHOD_NOT_ALLOWED, new RemoteControlResponse<Void>("You can used only GET and POST requests")));
            return;
        }
        Map<String, String> params = getParamsFromUri(msg.uri());
        String accessToken = params.get("token");
        if (accessToken == null || accessToken.isEmpty()) {
            sendHttpResponse(ctx, simpleJsonResponse(HttpResponseStatus.BAD_REQUEST, new RemoteControlResponse<Void>("Missing required parameter: token")));
            return;
        }
        RemoteControlConfig.RemoteControlToken token = module.config.find(accessToken);
        if (token == null) {
            sendHttpResponse(ctx, simpleJsonResponse(HttpResponseStatus.FORBIDDEN, new RemoteControlResponse<Void>("Token not valid")));
            return;
        }
        String command;
        if (token.allowAll) {
            command = params.get("command");
            if (command == null) {
                sendHttpResponse(ctx, simpleJsonResponse(HttpResponseStatus.BAD_REQUEST, new RemoteControlResponse<Void>("Missing required parameter: command")));
                return;
            }
        } else {
            command = params.get("command");
            if (command == null) {
                if (token.commands.size() != 1) {
                    sendHttpResponse(ctx, simpleJsonResponse(HttpResponseStatus.BAD_REQUEST, new RemoteControlResponse<Void>("Missing required parameter: command")));
                    return;
                }
                command = token.commands.get(0);
            }
            String finalCommand = command;
            if (token.startWithMode ? token.commands.stream().noneMatch(finalCommand::startsWith) : !token.commands.contains(command)) {
                sendHttpResponse(ctx, simpleJsonResponse(HttpResponseStatus.FORBIDDEN, new RemoteControlResponse<Void>("You cannot execute this command")));
                return;
            }
        }
        logger.info("[RemoteControl][Web] IP {} execute command '{}' with token {}...", context.ip, command, accessToken.substring(0, 5));
        boolean needLog = params.get("log") != null && Boolean.parseBoolean(params.get("log"));
        LogLinesContainer container = null;
        if (needLog) {
            container = new LogLinesContainer();
            LogAppender.getInstance().addListener(container);
        }
        String exception = null;
        try {
            server.commandHandler.evalNative(command, false);
        } catch (Throwable e) {
            if (needLog) {
                LogAppender.getInstance().removeListener(container);
            }
            logger.error(e);
            exception = e.toString();
        }
        SuccessCommandResponse response = new SuccessCommandResponse();
        response.exception = exception;
        response.lines = container == null ? null : container.lines;
        response.success = exception == null;
        if (needLog) {
            LogAppender.getInstance().removeListener(container);
        }
        sendHttpResponse(ctx, simpleJsonResponse(HttpResponseStatus.OK, new RemoteControlResponse<>(response)));
    }

    public static class RemoteControlResponse<T> {
        public String error;
        public T data;

        public RemoteControlResponse(String error) {
            this.error = error;
        }

        public RemoteControlResponse(T data) {
            this.data = data;
        }
    }

    public static class LogLinesContainer implements Consumer<LogEvent> {
        public List<LogEventView> lines = new ArrayList<>();

        @Override
        public void accept(LogEvent logEvent) {
            lines.add(new LogEventView(logEvent));
        }
    }

    public static class LogEventView {
        public final String level;
        public final String message;
        public final String exception;

        public LogEventView(LogEvent event) {
            level = event.getLevel().toString();
            message = event.getMessage().getFormattedMessage();
            Throwable throwable = event.getMessage().getThrowable();
            if (throwable != null) {
                exception = String.format("%s: %s", throwable.getClass().getName(), throwable.getMessage());
            } else {
                exception = null;
            }
        }
    }

    public static class SuccessCommandResponse {
        public List<LogEventView> lines;
        public String exception;
        public boolean success;
    }
}

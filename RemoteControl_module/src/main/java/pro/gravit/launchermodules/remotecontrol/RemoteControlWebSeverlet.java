package pro.gravit.launchermodules.remotecontrol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.NettyConnectContext;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;
import pro.gravit.utils.helper.LogHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteControlWebSeverlet implements NettyWebAPIHandler.SimpleSeverletHandler {
    private final RemoteControlModule module;
    private final LaunchServer server;

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
        LogHelper.info("[RemoteControl][Web] IP %s execute command '%s' with token %s...", context.ip, command, accessToken.substring(0, 5));
        boolean needLog = params.get("log") != null && Boolean.parseBoolean(params.get("log"));
        LogLinesContainer container = null;
        LogHelper.OutputEnity entity = null;
        if (needLog) {
            container = new LogLinesContainer();
            entity = new LogHelper.OutputEnity(container, LogHelper.OutputTypes.PLAIN);
            LogHelper.addOutput(entity);
        }
        String exception = null;
        try {
            server.commandHandler.evalNative(command, false);
        } catch (Throwable e) {
            if (needLog) {
                LogHelper.removeOutput(entity);
            }
            LogHelper.error(e);
            exception = e.toString();
        }
        SuccessCommandResponse response = new SuccessCommandResponse();
        response.exception = exception;
        response.lines = container == null ? null : container.lines;
        response.success = exception == null;
        if (needLog) {
            LogHelper.removeOutput(entity);
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

    public static class LogLinesContainer implements LogHelper.Output {
        public List<String> lines = new ArrayList<>();

        @Override
        public void println(String message) {
            lines.add(message);
        }
    }

    public static class SuccessCommandResponse {
        public List<String> lines;
        public String exception;
        public boolean success;
    }
}

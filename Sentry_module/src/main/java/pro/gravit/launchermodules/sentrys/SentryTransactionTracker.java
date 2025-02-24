package pro.gravit.launchermodules.sentrys;

import io.netty.channel.Channel;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import io.sentry.protocol.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.events.request.ErrorRequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.launchserver.socket.handlers.NettyServerSocketHandler;

import java.util.HashMap;
import java.util.Map;

public class SentryTransactionTracker {
    private final Logger logger = LogManager.getLogger(SentryTransactionTracker.class);
    private final SentryModule module;
    private final ThreadLocal<WebSocketService.WebSocketRequestContext> contextThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Object> latestSendEvent = new ThreadLocal<>();
    private final ThreadLocal<ITransaction> sentryTransaction = new ThreadLocal<>();

    public SentryTransactionTracker(SentryModule module) {
        this.module = module;
    }

    public void register(NettyServerSocketHandler handler) {
        handler.nettyServer.service.hookBeforeParsing.registerHook(this::onBeforeParsing);
        handler.nettyServer.service.hookBeforeExecute.registerHook(this::onBeforeExecute);
        handler.nettyServer.service.hookComplete.registerHook(this::onComplete);
        handler.nettyServer.service.hookSend.registerHook(this::onSend);
    }

    protected boolean onBeforeParsing(WebSocketService.WebSocketRequestContext context) {
        var before = contextThreadLocal.get();
        if(before != null) {
            Sentry.configureScope(scope -> {
                Sentry.captureException(new IncompleteRequestException());
                if(scope.getTransaction() != null) {
                    scope.getTransaction().finish(SpanStatus.DEADLINE_EXCEEDED);
                }
            });
        }
        Sentry.configureScope(scope -> {
            scope.setUser(makeSentryUser(context.client, context.ip));
            Map<String, String> security = new HashMap<>();
            security.put("type", context.client.type.name());
            security.put("isAuth", String.valueOf(context.client.isAuth));
            security.put("isLauncherCheck", String.valueOf(context.client.checkSign));
            security.put("trustLevel", makeTrustLevel(context.client.trustLevel));
            scope.setContexts("security", security);
            if(module.c.captureRequestData) {
                scope.setContexts("requestData", context.text);
            }
        });
        contextThreadLocal.set(context);
        return false;
    }

    protected boolean onBeforeExecute(WebSocketService.WebSocketRequestContext context) {
        if(context.response != null) {
            sentryTransaction.set(Sentry.startTransaction(context.response.getType(), "process"));
        } else {
            sentryTransaction.set(Sentry.startTransaction("unknown", "process"));
        }
        return false;
    }

    protected boolean onComplete(WebSocketService.WebSocketRequestContext context) {
        var before = contextThreadLocal.get();
        if(before != null) {
            var lastObj = latestSendEvent.get();
            if(module.c.captureRequestError && lastObj instanceof ErrorRequestEvent errorEvent) {
                Sentry.captureException(new RequestError(errorEvent.error));
            }
            var transaction = sentryTransaction.get();
            if(transaction != null) {
                if(context.exception == null) {
                    transaction.finish(SpanStatus.OK);
                } else if(context.response == null) {
                    transaction.finish(SpanStatus.UNIMPLEMENTED);
                } else {
                    transaction.finish(SpanStatus.UNKNOWN_ERROR);
                }
                sentryTransaction.remove();
            }
        }
        contextThreadLocal.remove();
        latestSendEvent.remove();
        return false;
    }

    protected boolean onSend(Channel channel, Object obj) {
        var context = contextThreadLocal.get();
        if(context != null && context.context.channel() == channel) {
            latestSendEvent.set(obj);
        }
        return false;
    }

    protected String makeTrustLevel(Client.TrustLevel trustLevel) {
        if(trustLevel == null) {
            return "null";
        }
        if(trustLevel.hardwareInfo != null) {
            return "hardware";
        }
        if(trustLevel.keyChecked) {
            return "publicKey";
        }
        return "none";
    }

    protected User makeSentryUser(Client client, String ip) {
        User user = new User();
        user.setIpAddress(ip);
        if(client.isAuth) {
            user.setUsername(client.username);
            user.setId(client.uuid.toString());
            Map<String, String> data = new HashMap<>();
            data.put("auth_id", client.auth_id);
            if(client.permissions != null) {
                if(client.permissions.getPerms() != null) {
                    data.put("permissions", String.join(",", client.permissions.getPerms()));
                }
                if(client.permissions.getRoles() != null) {
                    data.put("roles", String.join(",", client.permissions.getRoles()));
                }
            }
            user.setData(data);
        }
        return user;
    }

    public static class RequestError extends Exception {
        public RequestError() {
        }

        public RequestError(String message) {
            super(message);
        }
    }

    public static class IncompleteRequestException extends Exception {
        public IncompleteRequestException() {
        }

        public IncompleteRequestException(String message) {
            super(message);
        }
    }
}

package pro.gravit.launchermodules.sashoksupport.socket.legacy;

import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;
import pro.gravit.launchermodules.sashoksupport.socket.RequestType;
import pro.gravit.launchermodules.sashoksupport.socket.legacy.update.LauncherResponse;
import pro.gravit.launchermodules.sashoksupport.socket.legacy.update.LegacyLauncherResponse;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Response {
    private static final Map<Integer, Factory<?>> RESPONSES = new ConcurrentHashMap<>(8);
    protected final LegacyServerComponent component;
    protected final HInput input;
    protected final HOutput output;
    protected final long session;


    protected Response(LegacyServerComponent component, long session, HInput input, HOutput output) {
        this.component = component;
        this.input = input;
        this.output = output;
        this.session = session;
    }

    public static Response getResponse(int type, LegacyServerComponent component, long session, HInput input, HOutput output) {
        return RESPONSES.get(type).newResponse(component, session, input, output);
    }

    public static void registerResponse(int type, Factory<?> factory) {
        RESPONSES.put(type, factory);
    }

    public static void registerResponses() {
        registerResponse(RequestType.PING.getNumber(), PingResponse::new);
        registerResponse(RequestType.LEGACYLAUNCHER.getNumber(), LegacyLauncherResponse::new);
        registerResponse(RequestType.LAUNCHER.getNumber(), LauncherResponse::new);
    }

    public static void requestError(String message) throws RequestException {
        throw new RequestException(message);
    }

    protected static void writeNoError(HOutput output) throws IOException {
        output.writeString("", 0);
    }

    protected final void debug(String message) {
        LogHelper.subDebug("#%d %s", session, message);
    }

    public abstract void reply() throws Exception;


    @FunctionalInterface
    public interface Factory<R> {

        Response newResponse(LegacyServerComponent component, long id, HInput input, HOutput output);
    }
}

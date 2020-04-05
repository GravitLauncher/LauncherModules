package pro.gravit.launchermodules.sashoksupport.socket;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchermodules.sashoksupport.socket.legacy.Response;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public final class ResponseThread implements Runnable {
    private final Socket socket;
    private final ServerSocketHandler handler;

    public ResponseThread(ServerSocketHandler handler, Socket socket) throws SocketException {
        this.socket = socket;
        // Fix socket flags
        IOHelper.setSocketFlags(socket);
        this.handler = handler;
    }

    private Handshake readHandshake(HInput input, HOutput output) throws IOException {
        boolean legacy = false;
        long session = 0;
        // Verify magic number
        int magicNumber = input.readInt();
        if (magicNumber != Launcher.PROTOCOL_MAGIC)
            if (magicNumber == Launcher.PROTOCOL_MAGIC_LEGACY - 1) { // Previous launcher protocol
                session = 0;
                legacy = true;
            } else if (magicNumber == ServerSocketHandler.LEGACY_LAUNCHER_MAGIC) { // Previous launcher protocol
                session = 0;
                legacy = true;
            } else if (magicNumber == Launcher.PROTOCOL_MAGIC_LEGACY) {

            } else
                throw new IOException("Invalid Handshake");
        if (!legacy) {
            session = input.readLong();
        }
        int type = input.readVarInt();
        if (!handler.onHandshake(session, type)) {
            output.writeBoolean(false);
            return null;
        }

        // Protocol successfully verified
        output.writeBoolean(true);
        output.flush();
        return new Handshake(type, session);
    }

    private void respond(Integer type, HInput input, HOutput output, long session) throws Exception {
        if (handler.logConnections)
            LogHelper.info("Connection #%d", session);

        // Choose response based on type
        Response response = Response.getResponse(type, handler.component, session, input, output);

        // Reply
        response.reply();
        LogHelper.subDebug("#%d Replied", session);
    }

    @Override
    public void run() {
        if (!handler.logConnections)
            LogHelper.debug("Connection from %s", IOHelper.getIP(socket.getRemoteSocketAddress()));

        // Process connection
        boolean cancelled = false;
        Exception savedError = null;
        try (HInput input = new HInput(socket.getInputStream());
             HOutput output = new HOutput(socket.getOutputStream())) {
            Handshake handshake = readHandshake(input, output);
            if (handshake == null) { // Not accepted
                cancelled = true;
                return;
            }

            // Start response
            try {
                respond(handshake.type, input, output, handshake.session);
            } catch (RequestException e) {
                LogHelper.subDebug(String.format("#%d Request error: %s", handshake.session, e.getMessage()));
                if (e.getMessage() == null) LogHelper.error(e);
                output.writeString(e.getMessage(), 0);
            }
        } catch (Exception e) {
            savedError = e;
        } finally {
            IOHelper.close(socket);
            if (!cancelled)
                handler.onDisconnect(savedError);
        }
    }

    static class Handshake {
        final int type;
        final long session;

        public Handshake(int type, long session) {
            this.type = type;
            this.session = session;
        }
    }
}

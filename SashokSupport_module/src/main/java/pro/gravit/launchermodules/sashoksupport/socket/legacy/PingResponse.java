package pro.gravit.launchermodules.sashoksupport.socket.legacy;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.SerializeLimits;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.socket.Client;

import java.io.IOException;

public final class PingResponse extends Response {
    public PingResponse(LaunchServer server, long id, HInput input, HOutput output) {
        super(server, id, input, output);
    }

    @Override
    public void reply() throws IOException {
        output.writeUnsignedByte(SerializeLimits.EXPECTED_BYTE);
    }
}

package pro.gravit.launchermodules.sashoksupport.socket.legacy;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;

import java.io.IOException;

public final class PingResponse extends Response {
    public static final byte EXPECTED_BYTE = 0b01010101;

    public PingResponse(LegacyServerComponent component, long id, HInput input, HOutput output) {
        super(component, id, input, output);
    }

    @Override
    public void reply() throws IOException {
        output.writeUnsignedByte(EXPECTED_BYTE);
    }
}

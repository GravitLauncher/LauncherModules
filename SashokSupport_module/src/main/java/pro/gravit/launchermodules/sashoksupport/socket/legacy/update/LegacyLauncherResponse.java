package pro.gravit.launchermodules.sashoksupport.socket.legacy.update;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchermodules.sashoksupport.socket.EFileInfo;
import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;
import pro.gravit.launchermodules.sashoksupport.socket.legacy.Response;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;

public final class LegacyLauncherResponse extends Response {

    private static final int RSA_KEY_LENGTH_BITS = 2048;
    private static final int RSA_KEY_LENGTH = RSA_KEY_LENGTH_BITS / Byte.SIZE;

    public LegacyLauncherResponse(LegacyServerComponent component, long session, HInput input, HOutput output) {
        super(component, session, input, output);
    }

    @Override
    public void reply() throws IOException {
        EFileInfo curr = input.readBoolean() ? component.launcherEXE : component.launcher;
        writeNoError(output);
        output.writeByteArray(curr.sign, -RSA_KEY_LENGTH);
        output.flush();
        if (input.readBoolean()) {
            output.writeLength(curr.len, 0);
            IOHelper.transfer(curr.path, output.stream);
            return; // Launcher will be restarted
        }
        requestError("You must update");
    }
}

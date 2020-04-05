package pro.gravit.launchermodules.sashoksupport.socket.legacy.update;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchermodules.sashoksupport.socket.EFileInfo;
import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;
import pro.gravit.launchermodules.sashoksupport.socket.legacy.Response;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.util.Arrays;

public final class LauncherResponse extends Response {

    public static final int MAX_DIGEST = 512;

    public LauncherResponse(LegacyServerComponent component, long session, HInput input, HOutput output) {
        super(component, session, input, output);
    }

    @Override
    public void reply() throws IOException {
        EFileInfo curr = input.readBoolean() ? component.launcherEXE : component.launcher;
        if (!Arrays.equals(input.readByteArray(MAX_DIGEST), curr.digest)) {
            writeNoError(output);
            output.writeBoolean(true);
            output.writeLength(curr.len, 0);
            IOHelper.transfer(curr.path, output.stream);
            return; // Launcher will be restarted
        }
        requestError("You must update");
    }
}

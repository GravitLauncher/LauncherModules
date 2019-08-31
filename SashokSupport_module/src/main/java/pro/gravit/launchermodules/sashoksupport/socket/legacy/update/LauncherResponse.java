package pro.gravit.launchermodules.sashoksupport.socket.legacy.update;

import java.io.IOException;
import java.util.Arrays;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launcher.serialize.SerializeLimits;
import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;
import pro.gravit.launchermodules.sashoksupport.socket.legacy.Response;
import pro.gravit.utils.helper.IOHelper;

public final class LauncherResponse extends Response {

    public LauncherResponse(LegacyServerComponent component, long session, HInput input, HOutput output) {
        super(component, session, input, output);
    }

    @Override
    public void reply() throws IOException {
        // Resolve launcher binary
        boolean isExe = input.readBoolean();
        byte[] bytes = (isExe ? component.launchServer.launcherEXEBinary : component.launchServer.launcherBinary).getDigest();
        if (bytes == null) {
            requestError("Missing launcher binary");
            return;
        }
        byte[] digest = input.readByteArray(SerializeLimits.MAX_DIGEST);
        if (!Arrays.equals(bytes, digest)) {
            writeNoError(output);
            output.writeBoolean(true);
            output.writeByteArray(IOHelper.read(isExe ? component.launcher : component.launcherEXE), 0);
            return;
        }
        writeNoError(output);
        output.writeBoolean(false);
    }
}

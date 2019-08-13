package pro.gravit.launchermodules.sashoksupport.socket.legacy.update;

import pro.gravit.launcher.serialize.HInput;
import pro.gravit.launcher.serialize.HOutput;
import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;
import pro.gravit.launchermodules.sashoksupport.socket.legacy.Response;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;

public final class LegacyLauncherResponse extends Response {

    public LegacyLauncherResponse(LegacyServerComponent component, long session, HInput input, HOutput output) {
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
        writeNoError(output);

        // Update launcher binary
        output.writeByteArray(isExe ? component.launchServer.launcherEXEBinary.getSign() : component.launchServer.launcherBinary.getSign(), -SecurityHelper.RSA_KEY_LENGTH);
        output.flush();
        if (input.readBoolean()) {
            output.writeByteArray(IOHelper.read(isExe ? component.launcherEXE : component.launcher), 0);
            return; // Launcher will be restarted
        }
        requestError("You must update");
    }
}

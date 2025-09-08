package pro.gravit.launchermodules.mirrorhelper.newforge;

import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Lwjgl3ifyProfileModifier extends ProfileModifier {
    public Lwjgl3ifyProfileModifier(ClientProfile profile, Path clientDir) {
        super(profile, clientDir);
    }

    @Override
    public void apply(ClientProfileBuilder builder) throws IOException {
        super.apply(builder);
        {
            Path launchwrapperPath = clientDir.resolve("libraries/net/minecraft/launchwrapper");
            if(Files.exists(launchwrapperPath)) {
                IOHelper.deleteDir(launchwrapperPath, true);
            }
        }
    }
}

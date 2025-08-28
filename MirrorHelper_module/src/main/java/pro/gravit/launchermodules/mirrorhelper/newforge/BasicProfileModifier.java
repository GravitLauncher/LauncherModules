package pro.gravit.launchermodules.mirrorhelper.newforge;

import pro.gravit.launcher.base.profiles.ClientProfile;

import java.nio.file.Path;

public class BasicProfileModifier extends ProfileModifier {
    public BasicProfileModifier(ClientProfile profile, Path clientDir) {
        super(profile, clientDir);
    }
}

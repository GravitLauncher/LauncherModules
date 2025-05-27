package pro.gravit.launchermodules.mirrorhelper.newforge;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launchermodules.mirrorhelper.helpers.ClientToolkit;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CleanroomProfileModifier {
    private final ForgeProfile forgeProfile;
    private final ClientProfile profile;
    private final Path clientDir;

    public CleanroomProfileModifier(Path forgeProfilePath, ClientProfile profile, Path clientDir) {
        try(Reader reader = IOHelper.newReader(forgeProfilePath)) {
            this.forgeProfile = Launcher.gsonManager.gson.fromJson(reader, ForgeProfile.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.profile = profile;
        this.clientDir = clientDir;
    }

    public ClientProfile build() {
        ClientProfileBuilder builder = new ClientProfileBuilder(profile);
        builder.setMainClass(forgeProfile.mainClass());
        builder.setClassLoaderConfig(ClientProfile.ClassLoaderConfig.LAUNCHER);

        List<String> clientArgs = new ArrayList<>();
        clientArgs.addAll(ClientToolkit.findValuesForKey(forgeProfile.minecraftArguments(), "tweakClass"));
        clientArgs.addAll(ClientToolkit.findValuesForKey(forgeProfile.minecraftArguments(), "versionType"));
        builder.setClientArgs(clientArgs);
        builder.setRecommendJavaVersion(21);
        builder.setMinJavaVersion(21);
        return builder.createClientProfile();
    }
}

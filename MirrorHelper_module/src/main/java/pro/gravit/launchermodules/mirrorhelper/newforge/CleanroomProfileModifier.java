package pro.gravit.launchermodules.mirrorhelper.newforge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.launchermodules.mirrorhelper.helpers.ClientToolkit;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CleanroomProfileModifier extends ProfileModifier {
    private final ForgeProfile forgeProfile;

    public CleanroomProfileModifier(Path forgeProfilePath, ClientProfile profile, Path clientDir) {
        super(profile, clientDir);
        try(Reader reader = IOHelper.newReader(forgeProfilePath)) {
            this.forgeProfile = Launcher.gsonManager.gson.fromJson(reader, ForgeProfile.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void apply(ClientProfileBuilder builder) throws IOException {
        super.apply(builder);
        builder.setMainClass(forgeProfile.mainClass());
        builder.setClassLoaderConfig(ClientProfile.ClassLoaderConfig.LAUNCHER);
        List<String> clientArgs = new ArrayList<>();
        clientArgs.addAll(ClientToolkit.findValuesForKey(forgeProfile.minecraftArguments(), "tweakClass"));
        clientArgs.addAll(ClientToolkit.findValuesForKey(forgeProfile.minecraftArguments(), "versionType"));
        builder.setClientArgs(clientArgs);
        List<String> jvmArgs = new ArrayList<>(profile.getJvmArgs());
        jvmArgs.add("--add-opens");
        jvmArgs.add("java.base/java.lang.invoke=ALL-UNNAMED");
        builder.setJvmArgs(jvmArgs);
        builder.setFlags(List.of(ClientProfile.CompatibilityFlags.ENABLE_HACKS));
        builder.setCompatClasses(List.of("com.gravitlauncher.compatpatches.patches.FoundationPatches"));
        builder.setRecommendJavaVersion(24);
        builder.setMinJavaVersion(24);
    }
}

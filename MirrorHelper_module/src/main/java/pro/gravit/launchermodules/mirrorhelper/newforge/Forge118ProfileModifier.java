package pro.gravit.launchermodules.mirrorhelper.newforge;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.launch.LaunchOptions;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class Forge118ProfileModifier extends ProfileModifier {
    private final ForgeProfile forgeProfile;
    private final ClientProfile profile;
    private final Path clientDir;
    public static List<String> exclusionList = List.of("AutoRenamingTool", "net/minecraft/client", "net/neoforged/neoforge", "libraries/net/neoforged/installertools");
    private static final List<String> prevArgsList = List.of("-p", "--add-modules", "--add-opens", "--add-exports");

    public Forge118ProfileModifier(Path forgeProfilePath, ClientProfile profile, Path clientDir) {
        super(profile, clientDir);
        try(Reader reader = IOHelper.newReader(forgeProfilePath)) {
            this.forgeProfile = Launcher.gsonManager.gson.fromJson(reader, ForgeProfile.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.profile = profile;
        this.clientDir = clientDir;
    }

    public boolean containsInExclusionList(String value) {
        for(var e : exclusionList) {
            if(value.contains(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply(ClientProfileBuilder builder) throws IOException {
        super.apply(builder);
        builder.setMainClass(forgeProfile.mainClass());
        List<String> cp = builder.getClassPath();
        Path librariesPath = clientDir.resolve("libraries");
        try(Stream<Path> stream = Files.walk(librariesPath)) {
            cp.addAll(stream
                    .filter(e -> e.getFileName().toString().endsWith(".jar"))
                    .map(e -> clientDir.relativize(e).toString())
                    .filter(e -> !containsInExclusionList(e)).toList());
        }
        builder.setClassPath(cp);
        builder.setClassLoaderConfig(ClientProfile.ClassLoaderConfig.SYSTEM_ARGS);
        builder.setFlags(List.of(ClientProfile.CompatibilityFlags.ENABLE_HACKS, ClientProfile.CompatibilityFlags.DONT_ADD_YOURSELF_TO_CLASSPATH_PROPERTY));
        LaunchOptions.ModuleConf conf = new LaunchOptions.ModuleConf();
        List<String> jvmArgs = new ArrayList<>(forgeProfile.arguments().jvm().stream().map(this::processPlaceholders).toList());
        AtomicReference<String> prevArg = new AtomicReference<>();
        jvmArgs.removeIf(arg -> {
            if(prevArgsList.contains(arg)) {
                prevArg.set(arg);
                return true;
            }
            if(prevArg.get() != null) {
                processArg(prevArg.get(), arg, conf);
                prevArg.set(null);
                return true;
            }
            return false;
        });
        jvmArgs.add("--add-opens");
        jvmArgs.add("java.base/java.lang.invoke=ALL-UNNAMED");
        builder.setJvmArgs(jvmArgs);
        builder.setClientArgs(new ArrayList<>(forgeProfile.arguments().game()));
//        List<String> compatClasses = new ArrayList<>();
//        for(var e : cp) {
//            if(e.toLowerCase().contains("filesystemfixer")) {
//                compatClasses.add("pro.gravit.compat.filesystem.FileSystemFixer");
//            }
//        }
//        builder.setCompatClasses(compatClasses);
        //builder.setCompatClasses(List.of("pro.gravit.compat.filesystem.FileSystemFixer"));
        builder.setModuleConf(conf);
    }

    private String processPlaceholders(String value) {
        return value.replace("${library_directory}", "libraries");
    }
}

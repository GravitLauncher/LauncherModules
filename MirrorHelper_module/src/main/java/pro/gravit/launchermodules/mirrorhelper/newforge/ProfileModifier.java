package pro.gravit.launchermodules.mirrorhelper.newforge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.ClientProfileBuilder;
import pro.gravit.utils.launch.LaunchOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ProfileModifier {
    protected final transient Logger logger = LogManager.getLogger();
    protected final ClientProfile profile;
    protected final Path clientDir;

    public ProfileModifier(ClientProfile profile, Path clientDir) {
        this.profile = profile;
        this.clientDir = clientDir;
    }

    public ClientProfile build() throws IOException {
        ClientProfileBuilder builder = new ClientProfileBuilder(profile);
        apply(builder);
        return builder.createClientProfile();
    }

    public void apply(ClientProfileBuilder builder) throws IOException {
        try {
            Files.deleteIfExists(clientDir.resolve("libraries/org/lwjgl/lwjgl/lwjgl"));
            Files.deleteIfExists(clientDir.resolve("libraries/org/lwjgl/lwjgl/lwjgl_util"));
        } catch (IOException e) {
            logger.error("Failed to delete old lwjgl libraries", e);
        }
    }

    protected void fixSlf4jLibraries(List<String> classpath) {
        boolean containsSlf4j2Impl = false;
        boolean containsSlf4j2Api = false;
        for(var e : classpath) {
            if(e.startsWith("libraries/org/apache/logging/log4j/log4j-slf4j2-impl")) {
                containsSlf4j2Impl = true;
            }
            if(e.startsWith("libraries/org/slf4j/slf4j-api/2.")) {
                containsSlf4j2Api = true;
            }
        }
        if(containsSlf4j2Impl && containsSlf4j2Api) {
            classpath.removeIf((e) -> e.startsWith("libraries/org/apache/logging/log4j/log4j-slf4j18-impl"));
        }
        if(containsSlf4j2Impl && !containsSlf4j2Api) {
            classpath.removeIf((e) -> e.startsWith("libraries/org/apache/logging/log4j/log4j-slf4j2-impl"));
        }
    }

    protected void processArg(String key, String value, LaunchOptions.ModuleConf conf) {
        switch (key) {
            case "-p" -> {
                String[] splited = value.split("\\$\\{classpath_separator}");
                conf.modulePath = new ArrayList<>(List.of(splited));
            }
            case "--add-modules" -> {
                String[] splited = value.split(",");
                conf.modules = new ArrayList<>(List.of(splited));
            }
            case "--add-opens" -> {
                String[] splited = value.split("=");
                if (conf.opens == null) {
                    conf.opens = new HashMap<>();
                }
                conf.opens.put(splited[0], splited[1]);
            }
            case "--add-exports" -> {
                String[] splited = value.split("=");
                if (conf.exports == null) {
                    conf.exports = new HashMap<>();
                }
                conf.exports.put(splited[0], splited[1]);
            }
        }
    }
}

package pro.gravit.launchermodules.guard;

import pro.gravit.launcher.client.runtime.client.DirBridge;
import pro.gravit.launcher.client.runtime.client.events.ClientExitPhase;
import pro.gravit.launcher.client.runtime.client.events.ClientGuiPhase;
import pro.gravit.launcher.client.runtime.client.events.ClientPreGuiPhase;
import pro.gravit.launcher.client.runtime.client.events.client.ClientProcessBuilderCreateEvent;
import pro.gravit.launcher.client.runtime.client.events.client.ClientProcessPreInvokeMainClassEvent;
import pro.gravit.launcher.start.ClientLauncherWrapper;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.start.ClientWrapperModule;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ModuleImpl extends LauncherModule implements ClientWrapperModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    public static final Config config = new Config();

    public ModuleImpl() {
        super(new LauncherModuleInfo("LauncherGuard", version, Integer.MAX_VALUE - 300, new String[]{"ClientLauncherCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, ClientPreGuiPhase.class);
        registerEvent(this::preProcess, ClientProcessBuilderCreateEvent.class);
        registerEvent(this::finish, ClientGuiPhase.class);
        registerEvent(this::exitPhase, ClientExitPhase.class);
        registerEvent(this::preMainClass, ClientProcessPreInvokeMainClassEvent.class);
    }

    public void preInit(ClientPreGuiPhase phase) {

    }

    public void preProcess(ClientProcessBuilderCreateEvent event) {
        JavaHelper.JavaVersion javaVersion = event.processBuilder.javaVersion;
        Path executeFile = unpackIfPossible(javaVersion);
        if(executeFile != null) {
            event.processBuilder.executeFile = executeFile;
            event.processBuilder.useLegacyJavaClassPathProperty = config.useClasspathProperty;
            event.processBuilder.systemEnv.put("JAVA_HOME", javaVersion.jvmDir.toAbsolutePath().toString());
        }
    }

    public Path unpackIfPossible(JavaHelper.JavaVersion javaVersion) {
        String key = Launcher.makeSpecialGuardDirName(javaVersion.arch, JVMHelper.OS_TYPE);
        if(config.files.get(key) == null || config.exeFile.get(key) == null) {
            LogHelper.warning("LauncherGuard disabled: OS or ARCH not supported");
            return null;
        }
        Path executeFile = unpack(key);
        if(executeFile == null || Files.notExists(executeFile)) {
            throw new SecurityException("Wrong configuration: exe not found");
        }
        return executeFile;
    }

    public void preMainClass(ClientProcessPreInvokeMainClassEvent event) {
    }

    public void exitPhase(ClientExitPhase exitPhase) {
    }

    public void finish(ClientGuiPhase context) {

    }

    private Path unpack(String key) {
        Path exePath = null;
        try {
            List<String> list = config.files.get(key);
            String exeFile = config.exeFile.get(key);
            Path dir = DirBridge.getGuardDir().resolve(key);
            for(String e : list) {
                String url = "guard/".concat(key).concat("/").concat(e);
                Path target;
                if(config.renameExeFile && e.equals(exeFile)) {
                    target = dir.resolve(Objects.requireNonNull(Launcher.getConfig().projectName).concat(".exe"));
                } else {
                    target = dir.resolve(e);
                }
                if(e.equals(exeFile)) {
                    exePath = target;
                }
                IOHelper.createParentDirs(target);
                UnpackHelper.unpack(IOHelper.getResourceURL(url), target);
            }
        } catch (Throwable e) {
            throw new SecurityException(e);
        }
        return exePath;
    }

    @Override
    public void wrapperPhase(ClientLauncherWrapper.ClientLauncherWrapperContext context) {
        if(!config.protectLauncher) {
            return;
        }
        Path executeFile = unpackIfPossible(context.javaVersion);
        if(executeFile != null) {
            context.executePath = executeFile.toAbsolutePath();
            context.useLegacyClasspathProperty = config.useClasspathProperty;
            Map<String, String> env = context.processBuilder.environment();
            env.put("JAVA_HOME", context.javaVersion.jvmDir.toAbsolutePath().toString());
        }
    }
}

package pro.gravit.launchermodules.sentryl;

import org.objectweb.asm.Type;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JarHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;

public class ModuleImpl extends LauncherModule {
    private static final String keepClass = Type.getInternalName(ModuleImpl.class);
    public static final Version version = new Version(1, 1, 0, 0, Version.Type.LTS);
    public Path config;

    public ModuleImpl() {
        super(new LauncherModuleInfo("SentryModule", version));
    }

    public void postInit(LaunchServerPostInitPhase phase) {
        LaunchServer context = phase.server;
        JsonConfigurable<Config> configurable = modulesConfigManager.getConfigurable(Config.class, moduleInfo.name, "SentryModuleConfig");
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            LogHelper.error(e);
            return;
        }
        MainBuildTask mainTask = context.launcherBinary.getTaskByClass(MainBuildTask.class).get();
        mainTask.preBuildHook.registerHook(ctx -> {
            ctx.clientModules.add("pro.gravit.launchermodules.sentryl.ClientModule");
            ctx.readerClassPath.add(new JarFile(IOHelper.getCodeSource(ModuleImpl.class).toFile()));
        });
        mainTask.postBuildHook.registerHook(ctx -> {
            ctx.pushJarFile(IOHelper.getCodeSource(ModuleImpl.class), (e) -> e.getName().startsWith("META-INF"), (e) -> true);
            ctx.pushBytes("sentry.config.json", configurable.toJsonString().getBytes(IOHelper.UNICODE_CHARSET));
        });
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }

    @Override
    public void init(LauncherInitContext launcherInitContext) {
        registerEvent(this::postInit, LaunchServerPostInitPhase.class);
    }
}

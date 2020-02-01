package pro.gravit.launchermodules.launchermoduleloader;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class LauncherModuleLoaderModule extends LauncherModule {
    private transient LaunchServer server;

    public LauncherModuleLoaderModule() {
        super(new LauncherModuleInfo("LauncherModuleLoader", new Version(1, 1, 0, 1, Version.Type.LTS)));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent((e) -> server = e.server, LaunchServerInitPhase.class);
        registerEvent(this::postInit, LaunchServerPostInitPhase.class);
    }

    protected final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar"))
                try (JarFile f = new JarFile(file.toFile())) {
                    String mainClass = f.getManifest().getMainAttributes().getValue("Module-Main-Class");
                    if (mainClass == null) {
                        LogHelper.error("In module %s MainClass not found", file.toString());
                    } else {
                        module_class.add(mainClass);
                        module_jars.add(file);
                    }
                }
            return super.visitFile(file, attrs);
        }
    }

    public final List<String> module_class = new ArrayList<>();
    public final List<Path> module_jars = new ArrayList<>();
    public Path modules_dir;

    public void postInit(LaunchServerPostInitPhase phase) {
        modules_dir = server.dir.resolve("launcher-modules");
        if (!IOHelper.isDir(modules_dir)) {
            try {
                Files.createDirectories(modules_dir);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        server.commandHandler.registerCommand("SyncLauncherModules", new SyncLauncherModulesCommand(this));
        MainBuildTask mainTask = server.launcherBinary.getTaskByClass(MainBuildTask.class).get();
        mainTask.preBuildHook.registerHook((buildContext) -> {
            buildContext.clientModules.addAll(module_class);
            for (Path file : module_jars) {
                buildContext.readerClassPath.add(new JarFile(file.toFile()));
            }
        });
        mainTask.postBuildHook.registerHook((buildContext) -> {
            for (Path file : module_jars) {
                LogHelper.debug("Put %s launcher module", file.toString());
                buildContext.pushJarFile(file, (e) -> false, (e) -> true);
            }
        });
        try {
            syncModules();
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public void syncModules() throws IOException {
        module_jars.clear();
        module_class.clear();
        IOHelper.walk(modules_dir, new ModulesVisitor(), false);
    }
}

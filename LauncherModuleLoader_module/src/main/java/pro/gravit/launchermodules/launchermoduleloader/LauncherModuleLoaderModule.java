package pro.gravit.launchermodules.launchermoduleloader;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
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
import java.util.HashSet;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;

public class LauncherModuleLoaderModule extends LauncherModule {
    private transient LaunchServer server;

    public LauncherModuleLoaderModule() {
        super(new LauncherModuleInfo("LauncherModuleLoader", new Version(1, 1, 0)));
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
        server.buildHookManager.registerHook((buildContext) -> {
            HashSet<String> fileList = new HashSet<>(buildContext.fileList);
            fileList.add("META-INF/MANIFEST.MF");
            for (Path file : module_jars) {
                try {
                    buildContext.data.reader.getCp().add(new JarFile(file.toFile()));
                } catch (IOException e) {
                    LogHelper.error(e);
                }
                LogHelper.debug("Put %s launcher module", file.toString());
                try (ZipInputStream input = new ZipInputStream(IOHelper.newInput(file))) {
                    buildContext.pushJarFile(input, fileList);
                } catch (IOException e) {
                    LogHelper.error(e);
                }
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
        for (String s : module_class) {
            server.buildHookManager.unregisterClientModuleClass(s);
        }
        module_class.clear();
        IOHelper.walk(modules_dir, new ModulesVisitor(), false);
        for (String s : module_class) {
            server.buildHookManager.registerClientModuleClass(s);
        }
    }
}

package pro.gravit.launchermodules.launchermoduleloader;

import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;

public class LauncherModuleLoaderModule implements Module {
    public static Version version = new Version(1,0,0);
    public transient LaunchServer server;

    protected final class ModulesVisitor extends SimpleFileVisitor<Path> {
        private ModulesVisitor() {
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.toFile().getName().endsWith(".jar"))
                try (JarFile f = new JarFile(file.toFile())) {
                    String mainClass = f.getManifest().getMainAttributes().getValue("Main-Class");
                    if(mainClass == null)
                    {
                        LogHelper.error("In module %s MainClass not found", file.toString());
                    }
                    else
                    {
                        module_class.add(mainClass);
                        module_jars.add(file);
                    }
                }
            return super.visitFile(file, attrs);
        }
    }

    public List<String> module_class = new ArrayList<>();
    public List<Path> module_jars = new ArrayList<>();
    public Path modules_dir;
    @Override
    public String getName() {
        return "LauncherModuleLoader";
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void init(ModuleContext context) {
    }

    @Override
    public void postInit(ModuleContext context) {
        if (context.getType() == ModuleContext.Type.LAUNCHSERVER)
        {
            LaunchServerModuleContext con = (LaunchServerModuleContext) context;
            LaunchServer server = con.launchServer;
            this.server = server;
            modules_dir = server.dir.resolve("launcher-modules");
            if(!IOHelper.isDir(modules_dir))
            {
                try {
                    Files.createDirectories(modules_dir);
                } catch (IOException e) {
                    LogHelper.error(e);
                }
            }
            server.commandHandler.registerCommand("SyncLauncherModules", new SyncLauncherModulesCommand(this));
            server.buildHookManager.registerHook((buildContext) -> {
                for(Path file : module_jars)
                {
                    LogHelper.debug("Put %s launcher module", file.toString());
                    try(ZipInputStream input = new ZipInputStream(IOHelper.newInput(file)))
                    {
                        buildContext.pushJarFile(input, buildContext.fileList);
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
    }

    @Override
    public void preInit(ModuleContext context) {

    }

    public void syncModules() throws IOException
    {
        module_jars.clear();
        for(String s : module_class)
        {
            server.buildHookManager.unregisterClientModuleClass(s);
        }
        module_class.clear();
        IOHelper.walk(modules_dir, new ModulesVisitor(), false);
        for(String s : module_class)
        {
            server.buildHookManager.registerClientModuleClass(s);
        }
    }

    @Override
    public void close() throws Exception {

    }
}

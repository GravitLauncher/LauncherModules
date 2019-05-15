package ru.gravit.launchermodules.jarsigner;

import ru.gravit.launcher.Launcher;
import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.Reloadable;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;
import ru.gravit.utils.Version;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

public class ModuleImpl implements Module, Reloadable  {
    public static final Version version = new Version(0, 1, 0, 0, Version.Type.EXPERIMENTAL);

    public static class Config {
		public String key = "myPathToKey";
		public String storepass = "pass";
		public String algo = "JKS";
		public String keyalias = "mykey";
		public String pass = "pass";
    }

    public Path configFile = null;
    public Config config = null;
    
    @Override
    public void close() {

    }

    @Override
    public String getName() {
        return "JarSigner";
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE+200;
    }

    @Override
    public void init(ModuleContext context1) {
    }

    @Override
    public void preInit(ModuleContext context1) {
    }

    @Override
    public void postInit(ModuleContext context1) {
    }

    @Override
    public void finish(ModuleContext context1) {
    	LaunchServerModuleContext context = ((LaunchServerModuleContext) context1);
    	configFile = context.modulesConfigManager.getModuleConfig("jar-signing");
        if (IOHelper.exists(configFile)) {
            try (Reader reader = IOHelper.newReader(configFile)) {
                config  = Launcher.gsonManager.configGson.fromJson(reader, Config.class);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        } else {
            LogHelper.debug("Create new jar signing config file");
            try (Writer writer = IOHelper.newWriter(configFile)) {
                config = new Config();
                Launcher.gsonManager.configGson.toJson(config, writer);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        context.launchServer.launcherBinary.tasks.add(new SignJarTask(context.launchServer, this));
    }

    @Override
    public void reload() {
        try (Reader reader = IOHelper.newReader(configFile )) {
            config = Launcher.gsonManager.configGson.fromJson(reader, Config.class);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }
}

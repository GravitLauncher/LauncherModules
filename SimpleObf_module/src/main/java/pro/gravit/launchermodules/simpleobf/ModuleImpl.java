package pro.gravit.launchermodules.simpleobf;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.binary.tasks.AdditionalFixesApplyTask;
import pro.gravit.launchserver.binary.tasks.TaskUtil;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class ModuleImpl implements Module {
    public static final Version version = new Version(0, 1, 0, 0, Version.Type.BETA);

    public static class Config {
        public boolean simpleIndy = true;
        public boolean stripNOP = true;
        public boolean certCheck = false;
    }

    public Path configFile = null;
    public Config config = null;
    public boolean certCheck = false;

    @Override
    public void close() {

    }

    @Override
    public String getName() {
        return "SimpleObf";
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE + 600;
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
        configFile = context.modulesConfigManager.getModuleConfig("simple-obf");
        if (IOHelper.exists(configFile)) {
            try (Reader reader = IOHelper.newReader(configFile)) {
                config = Launcher.gsonManager.configGson.fromJson(reader, Config.class);
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
        certCheck = context.launchServer.modulesManager.modules.stream().anyMatch(e -> e.getName().equals("JarSigner"));	
        TaskUtil.add(context.launchServer.launcherBinary.tasks, t -> t instanceof AdditionalFixesApplyTask, new SimpleObfTask(context.launchServer, this));
    }


    public void reload() {
        try (Reader reader = IOHelper.newReader(configFile)) {
            config = Launcher.gsonManager.configGson.fromJson(reader, Config.class);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }
}

package pro.gravit.launchermodules.simpleobf;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launchserver.binary.tasks.AdditionalFixesApplyTask;
import pro.gravit.launchserver.binary.tasks.AttachJarsTask;
import pro.gravit.launchserver.binary.tasks.TaskUtil;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(0, 1, 0, 0, Version.Type.BETA);

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
        if(initContext != null)
        {
            if(initContext instanceof LaunchServerInitContext)
            {
                finish(new LaunchServerFullInitEvent(((LaunchServerInitContext) initContext).server));
            }
        }
    }

    public static class Config {
        public boolean simpleIndy = true;
        public boolean stripNOP = true;
        public boolean certCheck = false;
		public boolean trollObf = true;
    }

    public Path configFile = null;
    public Config config = null;
    public boolean certCheck = false;

    public void finish(LaunchServerFullInitEvent event) {
        configFile = modulesConfigManager.getModuleConfig("simple-obf");
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
        certCheck = event.server.modulesManager.containsModule("JarSigner");
        TaskUtil.add(event.server.launcherBinary.tasks, t -> t instanceof AttachJarsTask, new SimpleObfTask(event.server, this));
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

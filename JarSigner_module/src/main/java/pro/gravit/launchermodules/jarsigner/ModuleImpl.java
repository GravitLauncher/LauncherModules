package pro.gravit.launchermodules.jarsigner;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class ModuleImpl extends LauncherModule {
	public ModuleImpl() {
		super(new LauncherModuleInfo("JarSigner", version, Integer.MIN_VALUE+200, new String[0]));
	}
	
    public static final Version version = new Version(0, 1, 0, 0, Version.Type.LTS);

    public static class Config {
        public String key = "myPathToKey";
        public String storepass = "pass";
        public String algo = "JKS";
        public String keyalias = "mykey";
        public String pass = "pass";
        public String signAlgo = "SHA256WITHRSA";
    }

    public Path configFile = null;
    public Config config = null;

	@Override
	public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerPostInitPhase.class);
	}
	public void finish(LaunchServerPostInitPhase context) {
        configFile = context.server.modulesManager.getConfigManager().getModuleConfig("jar-signing");
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
        context.server.launcherBinary.tasks.add(new SignJarTask(context.server, this));
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

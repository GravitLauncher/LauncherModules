package pro.gravit.launchermodules.sentrys;

import pro.gravit.utils.Version;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.sentry.Sentry;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(0, 1, 0, 0, Version.Type.LTS);

	public ModuleImpl() {
		super(new LauncherModuleInfo("SentryServerModule", version, Integer.MAX_VALUE-200, new String[0]));
	}

	private static final Gson GSON_P = new GsonBuilder().setPrettyPrinting().setLenient().create();
	
	public Config c = null;
	
	public static class Config {
		String dsn = "YOUR_DSN";
		boolean captureAll = false;
	}
	
	@Override
	public void init(LauncherInitContext initContext) {
		registerEvent(this::preInit, PreConfigPhase.class);
	}
	public void preInit(PreConfigPhase phase)
	{
		try {
			Path p = Paths.get("sentry.json");
			if (Files.isReadable(p)) {
				c = GSON_P.fromJson(IOHelper.decode(IOHelper.read(p)), Config.class);
			} else {
				Files.deleteIfExists(p);
				c = new Config();
				IOHelper.write(p, IOHelper.encode(GSON_P.toJson(c, Config.class)));
			}
			Sentry.init(c.dsn);
			if (c.captureAll)
				LogHelper.addOutput(Sentry::capture, LogHelper.OutputTypes.PLAIN);
		} catch (Throwable e) {
			LogHelper.error(e);
		}
	}
}


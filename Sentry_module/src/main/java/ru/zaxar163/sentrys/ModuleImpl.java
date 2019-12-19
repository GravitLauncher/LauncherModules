package ru.zaxar163.sentrys;

import pro.gravit.utils.Version;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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

	@Override
	public void init(LauncherInitContext initContext) {
		registerEvent(this::preInit, PreConfigPhase.class);
	}
	public void preInit(PreConfigPhase phase)
	{
		try {
			Sentry.init(IOHelper.decode(IOHelper.read(Paths.get("sentry.cfg"))));
			Map<String, String> conf = Arrays.stream(IOHelper.decode(IOHelper.read(Paths.get("sentrymodule.cfg"))).split("\n")).filter(e -> e != null).map (e -> e.trim()).filter(e -> !e.isEmpty() && !e.startsWith("#")).map(e -> e.split("=")).filter(e -> e.length > 1).collect(Collectors.toMap(e -> e[0], e -> e[1]));
			LogHelper.addExcCallback(Sentry::capture);
			if (conf.getOrDefault("captureAll", "false").equalsIgnoreCase("true"))
				LogHelper.addOutput(Sentry::capture, LogHelper.OutputTypes.PLAIN);
		} catch (Throwable e) {
			LogHelper.error(e);
		}
	}
}


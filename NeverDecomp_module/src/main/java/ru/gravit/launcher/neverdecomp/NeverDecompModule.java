package ru.gravit.launcher.neverdecomp;

import ru.gravit.utils.Version;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.asm.AntiDecomp;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;

public class NeverDecompModule implements Module {
	@Override
	public void close() {

	}

	@Override
	public String getName() {
		return "NeverDecomp";
	}

	@Override
	public Version getVersion() {
		return new Version(1, 0, 2, 0);
	}

	@Override
	public int getPriority() {
		return 1;
	}

	@Override
	public void init(ModuleContext context1) {
		if (context1.getType().equals(ModuleContext.Type.LAUNCHSERVER)) {
			LaunchServerModuleContext context = (LaunchServerModuleContext) context1;
			context.launchServer.buildHookManager.registerClassTransformer((src, name, e) -> {
				return AntiDecomp.expAntiDecomp(src, e.reader);
			});
		}
	}

	@Override
	public void preInit(ModuleContext context1) {
		
	}

	@Override
	public void postInit(ModuleContext context1) {
		
	}
}

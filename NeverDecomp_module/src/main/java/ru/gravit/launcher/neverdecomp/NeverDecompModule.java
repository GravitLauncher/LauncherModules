package ru.gravit.launcher.neverdecomp;

import ru.gravit.utils.Version;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;

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
	}

	@Override
	public void preInit(ModuleContext context1) {
		
	}

	@Override
	public void postInit(ModuleContext context1) {
		
	}
}

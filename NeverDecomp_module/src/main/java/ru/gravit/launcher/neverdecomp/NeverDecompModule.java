package ru.gravit.launcher.neverdecomp;

import ru.gravit.utils.Version;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launcher.neverdecomp.asm.AntiDecompileClassVisitor;
import ru.gravit.launcher.neverdecomp.asm.TransformerClass;
import ru.gravit.launcher.serialize.config.entry.BooleanConfigEntry;
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
		return new Version(1, 0, 1, 2);
	}

	@Override
	public int getPriority() {
		return 1;
	}

	@Override
	public void init(ModuleContext context1) {
		if (context1.getType().equals(ModuleContext.Type.LAUNCHSERVER)) {
			// Config may has boolean variable "hardAntiDecomp", which enables hard mode (needs -noverify to JVM)
			LaunchServerModuleContext context = (LaunchServerModuleContext) context1;
			final boolean hobf = context.launchServer.config.block.hasEntry("hardAntiDecomp") ? context.launchServer.config.block.getEntryValue("hardAntiDecomp", BooleanConfigEntry.class) : false;
			context.launchServer.buildHookManager.registerClassTransformer((src, name, e) -> {
				ClassReader classReader = new ClassReader(src);
				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				classReader.accept(new AntiDecompileClassVisitor(writer, hobf), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
				return writer.toByteArray();
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

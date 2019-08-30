package pro.gravit.launchermodules.discordrpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.Type;

import pro.gravit.launcher.modules.*;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JarHelper;
import pro.gravit.utils.helper.LogHelper;

public class ModuleImpl extends LauncherModule {
	private static final String keepClass = Type.getInternalName(ModuleImpl.class);
    public static final Version version = new Version(1, 1, 0, 0, Version.Type.LTS);
	public Path config = Paths.get("config/DiscordRPC/DiscordRPCConfig.json");

    public ModuleImpl() {
        super(new LauncherModuleInfo("DiscordRPC", version));
    }

    public void preInit(PreConfigPhase phase) {
    	Config.getOrCreate(config);
    }

    public void postInit(LaunchServerPostInitPhase phase) {
		LaunchServer context = phase.server;
		context.buildHookManager.registerClientModuleClass("pro.gravit.launchermodules.discordrpc.ClientModule");
		context.buildHookManager.registerHook(ctx -> {
			try (ZipInputStream in = IOHelper.newZipInput(IOHelper.getCodeSource(ModuleImpl.class))) {
				ctx.data.reader.getCp().add(new JarFile(IOHelper.getCodeSource(ModuleImpl.class).toFile()));
				ctx.output.putNextEntry(IOHelper.newZipEntry("rpc.config.json"));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				Writer w = new OutputStreamWriter(baos, IOHelper.UNICODE_CHARSET);
				Config.getOrCreate(config).write(w);
				w.flush();
				ctx.output.write(baos.toByteArray());
				JarHelper.zipWalk(in, (i, e) -> {
					if (!e.getName().startsWith("META-INF") && !e.getName().startsWith(keepClass)) {
						ctx.output.putNextEntry(IOHelper.newZipEntry(e));
						IOHelper.transfer(i, ctx.output);
						ctx.fileList.add(e.getName());
					}
				});
				ctx.fileList.add("rpc.config.json");
			} catch (IOException e) {
				LogHelper.error(e);
			}
		});
		config = modulesConfigManager.getModuleConfig("DiscordRPC");
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }

    @Override
    public void init(LauncherInitContext launcherInitContext) {
        registerEvent(this::preInit, PreConfigPhase.class);
        registerEvent(this::postInit, LaunchServerPostInitPhase.class);
    }
}

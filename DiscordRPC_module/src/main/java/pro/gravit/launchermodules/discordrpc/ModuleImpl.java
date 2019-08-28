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

import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JarHelper;
import pro.gravit.utils.helper.LogHelper;

public class ModuleImpl implements Module {
	private static final String keepClass = Type.getInternalName(ModuleImpl.class);
    public static final Version version = new Version(1, 0, 0, 0, Version.Type.LTS);
	public Path config = Paths.get("config/discordrpc.json");

    @Override
    public void close() {

    }

    @Override
    public String getName() {
        return "DiscordRPC";
    }

    @Override
    public Version getVersion() {
        return version;
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
    	Config.getOrCreate(config);
    }

    @Override
    public void postInit(ModuleContext context1) {
    	LaunchServerModuleContext context = (LaunchServerModuleContext) context1;
		context.launchServer.buildHookManager.registerClientModuleClass("pro.gravit.launchermodules.discordrpc.ClientModule");
		context.launchServer.buildHookManager.registerHook(ctx -> {
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
        config = context1.getModulesConfigManager().getModuleConfig(getName());
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }
}

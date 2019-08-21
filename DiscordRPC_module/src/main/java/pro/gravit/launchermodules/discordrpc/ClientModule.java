package pro.gravit.launchermodules.discordrpc;

import java.io.Reader;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.client.ClientLauncher;
import pro.gravit.launcher.client.ClientModuleContext;
import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class ClientModule implements Module {
    public static final Version version = new Version(1, 0, 0, 0, Version.Type.LTS);

	@Override
	public void close() throws Exception {
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
		return 0;
	}

	@Override
	public void init(ModuleContext context1) {
		if (context1 instanceof ClientModuleContext) {
			String title = Launcher.profile.getTitle();
			String nick = ClientLauncher.playerProfile.username;
			try (Reader r = IOHelper.newReader(ClientModule.class.getResource("rpc.config.json"))) {
				Config c = Config.read(r);
				c.firstLine = replace(c.firstLine, nick, title);
				c.secondLine = replace(c.secondLine, nick, title);
				c.largeText = replace(c.largeText, nick, title);
				c.smallText = replace(c.smallText, nick, title);
				DiscordRPC.onConfig(c);
			} catch (Throwable e) {
				LogHelper.error(e);
			}
		}
	}

	private String replace(String src, String nick, String title) {
		if (src == null) return null;
		return CommonHelper.replace(src, "user", nick, "profile", title);
	}

	@Override
	public void postInit(ModuleContext context) {
		
	}

	@Override
	public void preInit(ModuleContext context) {
	}

}

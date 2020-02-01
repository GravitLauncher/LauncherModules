package pro.gravit.launchermodules.discordrpc;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.client.ClientLauncher;
import pro.gravit.launcher.client.events.ClientLauncherInitPhase;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;

import java.io.Reader;

public class ClientModule extends LauncherModule {
    public static final Version version = new Version(1, 1, 0, 1, Version.Type.LTS);

    public ClientModule() {
        super(new LauncherModuleInfo("DiscordRPC", version));
    }

    @Override
    public void init(LauncherInitContext initContext) {

        registerEvent(this::clientInit, ClientLauncherInitPhase.class);
    }

    private String replace(String src, String nick, String title) {
        if (src == null) return null;
        return CommonHelper.replace(src, "user", nick, "profile", title);
    }

    private void clientInit(ClientLauncherInitPhase phase) {
        try {
            String title = Launcher.profile.getTitle();
            String nick = ClientLauncher.playerProfile.username;
            Config c;
            try (Reader r = IOHelper.newReader(ClientModule.class.getResource("/rpc.config.json"))) {
            	c = Config.read(r);
            }
            c.firstLine = replace(c.firstLine, nick, title);
            c.secondLine = replace(c.secondLine, nick, title);
            c.largeText = replace(c.largeText, nick, title);
            c.smallText = replace(c.smallText, nick, title);
            DiscordRPC.onConfig(c);
        } catch (Throwable ignored) {

        }
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s Launcher.");
    }

}

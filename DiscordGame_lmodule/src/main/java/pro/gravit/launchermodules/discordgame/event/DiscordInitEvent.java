package pro.gravit.launchermodules.discordgame.event;

import de.jcm.discordgamesdk.Core;
import pro.gravit.launcher.base.modules.LauncherModule;

public class DiscordInitEvent extends LauncherModule.Event {
    public final Core core;

    public DiscordInitEvent(Core core) {
        this.core = core;
    }
}

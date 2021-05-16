package pro.gravit.launcher.client.api;


import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.user.DiscordUser;
import pro.gravit.launchermodules.discordgame.DiscordBridge;

public class DiscordGameService {
    private DiscordGameService() {
        throw new UnsupportedOperationException();
    }

    public static Core getCore() {
        return DiscordBridge.getCore();
    }

    public static Activity getActivity() {
        return DiscordBridge.getActivity();
    }

    public static DiscordActivityService getDiscordActivityService() {
        return DiscordBridge.activityService;
    }

    public static DiscordUser getDiscordUser() {
        return DiscordBridge.getCore().userManager().getCurrentUser();
    }
}

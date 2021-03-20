package pro.gravit.launchermodules.discordgame;

import de.jcm.discordgamesdk.Core;
import pro.gravit.utils.helper.LogHelper;

public class DiscordUpdateTask implements Runnable {
    private final Core core;

    public DiscordUpdateTask(Core core) {
        this.core = core;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                core.runCallbacks();
            } catch (Throwable e) {
                LogHelper.error(e);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

    }
}

package pro.gravit.launchermodules.discordgame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.jcm.discordgamesdk.Core;
import pro.gravit.utils.helper.LogHelper;

public class DiscordUpdateTask implements Runnable {

    private static final Logger logger =
            LoggerFactory.getLogger(DiscordUpdateTask.class);

    private final Core core;

    public DiscordUpdateTask(Core core) {
        this.core = core;
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                core.runCallbacks();
            } catch (Throwable e) {
                logger.error("", e);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }

    }
}
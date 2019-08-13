package pro.gravit.launchermodules.statistics;

import java.util.Timer;
import java.util.TimerTask;

public class StatisticsManager {
    public int joinServerNumber;
    public int checkServerNumber;
    public int connectionNumber;
    public int authNumber;
    public int fullAuthNumber;

    long loadTime;
    private Timer timer = new Timer("StatisticManager", true);

    public StatisticsManager() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                joinServerNumber = 0;
                checkServerNumber = 0;
                connectionNumber = 0;
                authNumber = 0;
                fullAuthNumber = 0;
                loadTime = System.currentTimeMillis();
            }
        };
        long day_millis = 1000 * 60 * 60 * 24;
        timer.schedule(task, day_millis, day_millis);
    }
}

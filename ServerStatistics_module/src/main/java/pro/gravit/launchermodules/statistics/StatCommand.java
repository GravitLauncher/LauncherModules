package pro.gravit.launchermodules.statistics;

import pro.gravit.utils.command.Command;
import pro.gravit.utils.helper.LogHelper;

public class StatCommand extends Command {
    private final StatisticsManager manager;

    public StatCommand(StatisticsManager manager) {
        this.manager = manager;
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "Get LaunchServer statistics";
    }

    @Override
    public void invoke(String... args) {
        LogHelper.info(formatAverage("joinServer", manager.joinServerNumber, manager.loadTime));
        LogHelper.info(formatAverage("checkServer", manager.checkServerNumber, manager.loadTime));

        LogHelper.info(formatAverage("auth", manager.authNumber, manager.loadTime));
        LogHelper.info(formatAverage("fullAuth", manager.fullAuthNumber, manager.loadTime));
        LogHelper.info(formatAverage("connection", manager.connectionNumber, manager.loadTime));
    }

    public String formatAverage(String name, long value, long time) {
        long current_time = System.currentTimeMillis();
        long delay = current_time - time;
        double delayPerSecond = (double) delay / 1000;
        double delayPerMinute = (double) delay / 1000 / 60;
        double delayPerHour = (double) delay / 1000 / 60 / 60;

        if (delayPerSecond < 1) delayPerSecond = 1;
        if (delayPerMinute < 1) delayPerMinute = 1;
        if (delayPerHour < 1) delayPerHour = 1;

        double perSecond = (double) value / delayPerSecond;
        double perMinute = (double) value / delayPerMinute;
        double perHour = (double) value / delayPerHour;
        return String.format("[STAT] %s - %d (%f in s) (%f in m) (%f in h)", name, value, perSecond, perMinute, perHour);
    }
}

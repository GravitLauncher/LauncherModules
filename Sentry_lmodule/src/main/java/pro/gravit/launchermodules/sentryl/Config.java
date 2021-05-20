package pro.gravit.launchermodules.sentryl;

import pro.gravit.launcher.LauncherInject;

public class Config {
    @LauncherInject("modules.sentry.dsn")
    public String dsn;
    @LauncherInject("modules.sentry.captureAll")
    public boolean captureAll;
    @LauncherInject("modules.sentry.setThreadExcpectionHandler")
    public boolean setThreadExcpectionHandler;

    public static Object getDefault() {
        Config config = new Config();
        config.dsn = "YOUR_DSN";
        return new Config();
    }
}

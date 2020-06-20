package pro.gravit.launchermodules.sentryl;

import pro.gravit.launcher.LauncherInject;

public class Config {
    @LauncherInject("modules.sentry.dsn")
    public String dsn = "YOUR_DSN";
    @LauncherInject("modules.sentry.captureAll")
    public boolean captureAll = false;
    @LauncherInject("modules.sentry.setThreadExcpectionHandler")
    public boolean setThreadExcpectionHandler = false;
    public static Object getDefault() {
        return new Config();
    }
}

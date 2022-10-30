package pro.gravit.launchermodules.sentryl;

import pro.gravit.launcher.LauncherInject;

public class Config {
    @LauncherInject("modules.sentry.dsn")
    public String dsn;
    @LauncherInject("modules.sentry.captureall")
    public boolean captureAll;
    @LauncherInject("modules.sentry.setthreadexcpectionhandler")
    public boolean setThreadExcpectionHandler;

    public static Object getDefault() {
        Config config = new Config();
        config.dsn = "YOUR_DSN";
        return config;
    }
}

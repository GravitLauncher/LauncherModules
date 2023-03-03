package pro.gravit.launchermodules.sentryl;

import pro.gravit.launcher.LauncherInject;

public class Config {
    @LauncherInject("modules.sentry.dsn")
    public String dsn;
    @LauncherInject("modules.sentry.collectsysteminfo")
    public boolean collectSystemInfo;
    @LauncherInject("modules.sentry.collectmemoryinfo")
    public boolean collectMemoryInfo;

    public static Object getDefault() {
        Config config = new Config();
        config.dsn = "YOUR_DSN";
        config.collectSystemInfo = true;
        config.collectMemoryInfo = true;
        return config;
    }
}

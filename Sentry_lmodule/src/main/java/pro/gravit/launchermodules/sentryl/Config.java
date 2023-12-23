package pro.gravit.launchermodules.sentryl;

import pro.gravit.launcher.core.LauncherInject;

import java.util.ArrayList;
import java.util.List;

public class Config {
    @LauncherInject("modules.sentry.dsn")
    public String dsn;
    @LauncherInject("modules.sentry.collectsysteminfo")
    public boolean collectSystemInfo;
    @LauncherInject("modules.sentry.collectmemoryinfo")
    public boolean collectMemoryInfo;

    @LauncherInject("modules.sentry.ignoreerrors")
    public List<String> ignoreErrors;

    public static Object getDefault() {
        Config config = new Config();
        config.dsn = "YOUR_DSN";
        config.collectSystemInfo = true;
        config.collectMemoryInfo = true;
        config.ignoreErrors = new ArrayList<>();
        config.ignoreErrors.add("auth.wrongpassword");
        return config;
    }
}

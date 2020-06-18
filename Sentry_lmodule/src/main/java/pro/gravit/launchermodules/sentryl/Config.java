package pro.gravit.launchermodules.sentryl;

import com.google.gson.annotations.SerializedName;
import pro.gravit.launcher.LauncherInject;

public class Config {
    @SerializedName("dsn")
    @LauncherInject("modules.sentry.dsn")
    public String dsn = "YOUR_DSN";
    @SerializedName("captureAll")
    @LauncherInject("modules.sentry.captureAll")
    public boolean captureAll = false;

    public static Object getDefault() {
        return new Config();
    }
}

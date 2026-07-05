package pro.gravit.launchermodules.mdnsclient;

import pro.gravit.launcher.core.LauncherInject;

import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    @LauncherInject(value = "modules.mdnsclient.type")
    public String type;

    public static Object getDefault() {
        Config config = new Config();
        config.type = "_launchserver._tcp.local.";
        return config;
    }
}

package pro.gravit.launchermodules.startscreen;

import pro.gravit.launcher.LauncherInject;

public class Config {
    @LauncherInject(value = "modules.startscreen.imageurl")
    public String imageURL;
    @LauncherInject(value = "modules.startscreen.faviconURL")
    public String faviconURL;
    @LauncherInject(value = "modules.startscreen.colorr")
    public float colorR;
    @LauncherInject(value = "modules.startscreen.colorg")
    public float colorG;
    @LauncherInject(value = "modules.startscreen.colorb")
    public float colorB;
    @LauncherInject(value = "modules.startscreen.colora")
    public float colorA;

    public static Object getDefault() {
        Config config = new Config();
        config.imageURL = "runtime/splash.png";
        config.faviconURL = "runtime/favicon.ico";
        config.colorR = 1.0f;
        config.colorG = 1.0f;
        config.colorB = 1.0f;
        config.colorA = 0.0f;
        return config;
    }
}

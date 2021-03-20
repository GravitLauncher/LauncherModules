package pro.gravit.launchermodules.onelauncher;

import pro.gravit.launcher.LauncherInject;

public class OneLauncherConfig {
    @LauncherInject(value = "modules.onelauncher.text")
    public String text;
    @LauncherInject(value = "modules.onelauncher.launcherlock")
    public boolean launcherLock;
    @LauncherInject(value = "modules.onelauncher.clientlock")
    public boolean clientLock;
    @LauncherInject(value = "modules.onelauncher.checkclientlock")
    public boolean checkClientLock;
    @LauncherInject(value = "modules.onelauncher.multipleprofilesallowlock")
    public boolean multipleProfilesAllow;

    public static Object getDefault() {
        OneLauncherConfig config = new OneLauncherConfig();
        config.text = "Launcher or minecraft is already running";
        config.launcherLock = true;
        config.clientLock = true;
        config.checkClientLock = true;
        config.multipleProfilesAllow = false;
        return config;
    }
}

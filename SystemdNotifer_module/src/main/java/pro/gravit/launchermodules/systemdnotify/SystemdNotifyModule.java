package pro.gravit.launchermodules.systemdnotify;

import java.io.IOException;

import pro.gravit.launcher.modules.*;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

public class SystemdNotifyModule extends LauncherModule {
    public static Version version = new Version(1, 0, 0);

    public SystemdNotifyModule() {
        super(new LauncherModuleInfo("SystemdNotifer", version));
    }

    public void finish(LaunchServerFullInitEvent event) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("systemd-notify", "--ready");
        try {
            processBuilder.start();
            LogHelper.debug("Systemd notify successful");
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
    }
}

package pro.gravit.launchermodules.systemdnotify;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class SystemdNotifyModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);

    public SystemdNotifyModule() {
        super(new LauncherModuleInfo("SystemdNotifer", version));
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
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

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
    }
}

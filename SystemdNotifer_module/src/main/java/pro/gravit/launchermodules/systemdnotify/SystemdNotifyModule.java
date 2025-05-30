package pro.gravit.launchermodules.systemdnotify;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

import java.io.IOException;

public class SystemdNotifyModule extends LauncherModule {
    private transient final Logger logger = LogManager.getLogger(SystemdNotifyModule.class);
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);

    public SystemdNotifyModule() {
        super(new LauncherModuleInfoBuilder().setName("SystemdNotifer").setVersion(version).setDependencies(new String[]{"LaunchServerCore"}).createLauncherModuleInfo());
    }


    public void finish(LaunchServerFullInitEvent event) {
        notifySystemd();
    }

    public void notifySystemd() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("systemd-notify", "--ready");
        processBuilder.inheritIO();
        try {
            var process = processBuilder.start();
            var exitCode = process.waitFor();
            logger.debug("Systemd notify successful. Exit code {}", exitCode);
        } catch (IOException | InterruptedException e) {
            logger.error("Systemd-notify error", e);
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
        if (initContext instanceof LaunchServerInitContext) {
            notifySystemd();
        }
    }
}

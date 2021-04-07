package pro.gravit.launchermodules.generatecertificate;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

public class GenerateCertificateModule extends LauncherModule {
    public GenerateCertificateModule() {
        super(new LauncherModuleInfo("GenerateCertificate", new Version(1, 0, 0), new String[]{"LaunchServerCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if (initContext instanceof LaunchServerInitContext) {
            initGenerateCertificate(((LaunchServerInitContext) initContext).server);
        } else {
            registerEvent(this::registerCommand, LaunchServerFullInitEvent.class);
        }
    }

    public void registerCommand(LaunchServerFullInitEvent event) {
        initGenerateCertificate(event.server);
    }

    public void initGenerateCertificate(LaunchServer server) {
        server.commandHandler.registerCommand("generatecertificate", new GenerateCertificateCommand(server));
    }
}

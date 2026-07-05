package pro.gravit.launchermodules.mdnsclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.LauncherConfig;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launcher.base.modules.events.ClosePhase;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.utils.Version;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MDNSClient extends LauncherModule {
    public static final Version version = new Version(1, 0, 2, 1, Version.Type.STABLE);
    private static boolean registred = false;
    private MDNSService service;
    private Logger logger = LoggerFactory.getLogger(MDNSClient.class);
    public Config config = new Config();

    public MDNSClient() {
        super(new LauncherModuleInfoBuilder().setName("mDNSClient").setVersion(version).setDependencies(new String[]{"ClientLauncherCore"}).createLauncherModuleInfo());
    }

    public void initMe(PreConfigPhase phase) {
        try {
            service = new MDNSService(config.type);
            String newAddress = service.getFuture().get(1500, TimeUnit.MILLISECONDS);
            LauncherConfig config1 = Launcher.getConfig();
            config1.address = newAddress;
        } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
            logger.error("Launch mDNS service failed", e);
        }
    }

    public void close(ClosePhase phase) {
        if(service != null) {
            try {
                service.close();
            } catch (Exception e) {
                logger.error("Failed to close mDNS service", e);
            }
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::initMe, PreConfigPhase.class);
        registerEvent(this::close, ClosePhase.class);
    }
}

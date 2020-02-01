package pro.gravit.launchermodules.sashoksupport;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchermodules.sashoksupport.socket.LegacyServerComponent;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.components.Component;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

public class ModuleImpl extends LauncherModule {
    private static boolean registred = false;
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);

    public ModuleImpl() {
        super(new LauncherModuleInfo("SashokSupport", version));
    }

    public void preInit(PreConfigPhase preConfigPhase) {
        if (!registred) {
            AuthHandler.providers.register("binaryFile", BinaryFileAuthHandler.class);
            Component.providers.register("legacyServer", LegacyServerComponent.class);
            registred = true;
        }
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, PreConfigPhase.class);
        if (initContext != null) {
            if (initContext instanceof LaunchServerInitContext) {
                preInit(new PreConfigPhase());
            }
        }
    }
}

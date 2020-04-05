package pro.gravit.launchermodules.addhash;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.utils.Version;

public class AddHashModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 2, 1, Version.Type.STABLE);
    private static boolean registred = false;

    public AddHashModule() {
        super(new LauncherModuleInfo("AddHash", version));
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }

    public void preInit(PreConfigPhase preConfigPhase) {
        if (!registred) {
            AuthProvider.providers.register("mysql-bcrypt", MySQLBcryptAuthProvider.class);
            AuthProvider.providers.register("mysql-phphash", MySQLPhpHashAuthProvider.class);
            registred = true;
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, PreConfigPhase.class);
    }
}

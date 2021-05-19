package pro.gravit.launchermodules.addhash;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchermodules.addhash.verifier.BCryptPasswordVerifier;
import pro.gravit.launchermodules.addhash.verifier.PhpHashPasswordVerifier;
import pro.gravit.launchserver.auth.password.PasswordVerifier;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.utils.Version;

public class AddHashModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 2, 1, Version.Type.STABLE);
    private static boolean registred = false;

    public AddHashModule() {
        super(new LauncherModuleInfo("AddHash", version, new String[]{"LaunchServerCore"}));
    }


    public void preInit(PreConfigPhase preConfigPhase) {
        if (!registred) {
            AuthProvider.providers.register("mysql-bcrypt", MySQLBcryptAuthProvider.class);
            AuthProvider.providers.register("mysql-phphash", MySQLPhpHashAuthProvider.class);
            PasswordVerifier.providers.register("bcrypt", BCryptPasswordVerifier.class);
            PasswordVerifier.providers.register("phpass", PhpHashPasswordVerifier.class);
            registred = true;
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, PreConfigPhase.class);
    }
}

package pro.gravit.launchermodules.addhash;

import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.utils.Version;

public class AddHashModule implements Module {
    private static boolean registred = false;
    public static final Version version = new Version(1, 0, 2, 1, Version.Type.LTS);

    @Override
    public void close() {

    }

    @Override
    public String getName() {
        return "AddHash";
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public void init(ModuleContext context1) {
    }

    @Override
    public void preInit(ModuleContext context1) {
        if (!registred) {
            AuthProvider.providers.register("mysql-bcrypt", MySQLBcryptAuthProvider.class);
            AuthProvider.providers.register("mysql-phphash", MySQLPhpHashAuthProvider.class);
            registred = true;
        }
    }

    @Override
    public void postInit(ModuleContext context1) {

    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }
}

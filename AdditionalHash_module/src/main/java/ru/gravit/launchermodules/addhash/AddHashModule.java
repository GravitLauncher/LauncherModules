package ru.gravit.launchermodules.addhash;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.utils.Version;

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
            AuthProvider.providers.registerProvider("mysql-bcrypt", MySQLBcryptAuthProvider.class);
            AuthProvider.providers.registerProvider("mysql-phphash", MySQLPhpHashAuthProvider.class);
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

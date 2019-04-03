package ru.gravit.launchermodules.legacysupport;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.auth.handler.AuthHandler;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.texture.TextureProvider;
import ru.gravit.utils.Version;

public class ModuleImpl implements Module {
    private static boolean registred = false;
    public static final Version version = new Version(1, 0, 0, 0, Version.Type.LTS);

    @Override
    public void close() {

    }

    @Override
    public String getName() {
        return "DepcreatedFunctions";
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
            AuthHandler.registerHandler("binaryFile", BinaryFileAuthHandler.class);
            AuthHandler.registerHandler("mojang", MojangAuthHandler.class);
            AuthProvider.registerProvider("mojang", MojangAuthProvider.class);
            TextureProvider.registerProvider("mojang", MojangTextureProvider.class);
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

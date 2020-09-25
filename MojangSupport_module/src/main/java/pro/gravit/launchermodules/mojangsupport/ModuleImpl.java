package pro.gravit.launchermodules.mojangsupport;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    private static boolean registred = false;

    public ModuleImpl() {
        super(new LauncherModuleInfo("LegacySupport", version, new String[]{"LaunchServerCore"}));
    }


    public void preInit(PreConfigPhase preConfigPhase) {
        if (!registred) {
            AuthHandler.providers.register("mojang", MojangAuthHandler.class);
            AuthProvider.providers.register("mojang", MojangAuthProvider.class);
            TextureProvider.providers.register("mojang", MojangTextureProvider.class);
            registred = true;
        }
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

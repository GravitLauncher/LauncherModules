package pro.gravit.launchermodules.mojangsupport;

import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    private static boolean registred = false;

    public ModuleImpl() {
        super(new LauncherModuleInfoBuilder().setName("LegacySupport").setVersion(version).setDependencies(new String[]{"LaunchServerCore"}).createLauncherModuleInfo());
    }


    public void preInit(PreConfigPhase preConfigPhase) {
        if (!registred) {
            AuthCoreProvider.providers.register("mojang", MojangAuthCoreProvider.class);
            AuthCoreProvider.providers.register("microsoft", MicrosoftAuthCoreProvider.class);
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

package pro.gravit.launchermodules.example.hybridmoule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.utils.Version;

public class HybridLauncherModule extends LauncherModule {
    private static final Logger logger = LoggerFactory.getLogger(HybridLauncherModule.class);

    public HybridLauncherModule() {
        super(new LauncherModuleInfoBuilder()
                .setName("ExampleHybridModule")
                .setVersion(new Version(1, 0, 0, 1, Version.Type.LTS))
                .createLauncherModuleInfo());
    }

    @Override
    public void init(LauncherInitContext initContext) {
        logger.info("ExampleHybridModule initialized: {}", ExampleClass.message());
    }
}

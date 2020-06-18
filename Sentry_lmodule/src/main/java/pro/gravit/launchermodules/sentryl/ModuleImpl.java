package pro.gravit.launchermodules.sentryl;

import io.sentry.Sentry;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 1, 0, 1, Version.Type.LTS);

    public ModuleImpl() {
        super(new LauncherModuleInfo("SentryModule", version));
    }


    @Override
    public void init(LauncherInitContext initContext) {

        registerEvent(this::clientInit, PreConfigPhase.class);
    }
    @LauncherInject("modules.sentry.dsn")
    public String dsn = "YOUR_DSN";
    @LauncherInject("modules.sentry.captureAll")
    public boolean captureAll = false;
    private void clientInit(PreConfigPhase phase) {
        try {
            Sentry.init(dsn);
            if (captureAll)
                LogHelper.addOutput(Sentry::capture, LogHelper.OutputTypes.PLAIN);
        } catch (Throwable e) {
            LogHelper.error(e);
        }
    }

}

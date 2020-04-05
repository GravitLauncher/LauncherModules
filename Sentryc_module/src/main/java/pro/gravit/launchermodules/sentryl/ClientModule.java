package pro.gravit.launchermodules.sentryl;

import io.sentry.Sentry;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.Reader;

public class ClientModule extends LauncherModule {
    public static final Version version = new Version(1, 1, 0, 1, Version.Type.LTS);

    public ClientModule() {
        super(new LauncherModuleInfo("SentryModule", version));
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s Launcher.");
    }

    @Override
    public void init(LauncherInitContext initContext) {

        registerEvent(this::clientInit, PreConfigPhase.class);
    }

    private void clientInit(PreConfigPhase phase) {
        try {
            Config c;
            try (Reader r = IOHelper.newReader(ClientModule.class.getResource("/sentry.config.json"))) {
                c = Config.read(r);
            }
            Sentry.init(c.dsn);
            if (c.captureAll)
                LogHelper.addOutput(Sentry::capture, LogHelper.OutputTypes.PLAIN);
        } catch (Throwable e) {
            LogHelper.error(e);
        }
    }

}

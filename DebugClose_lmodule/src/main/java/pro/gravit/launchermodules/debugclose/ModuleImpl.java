package pro.gravit.launchermodules.debugclose;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Timer;
import java.util.TimerTask;

public class ModuleImpl extends LauncherModule {
    public ModuleImpl() {
        super(new LauncherModuleInfo("DebugClose", version, Integer.MAX_VALUE - 300, new String[0]));
    }

    public static final Version version = new Version(0, 1, 0, 0, Version.Type.LTS);

    public Timer worker;
    public Thread watcher = null;

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, PreConfigPhase.class);
    }

    public void preInit(PreConfigPhase phase) {
        worker = new Timer(true);
        worker.schedule(new TimerTask() {
            @Override
            public void run() {
                Thread.getAllStackTraces().forEach((a, b) -> {
                    StringWriter sm = new StringWriter();
                    Exception ex = new Exception();
                    ex.setStackTrace(b);
                    ex.printStackTrace(new PrintWriter(sm));
                    LogHelper.debug(String.format("Therad name: %s, id: %d, Stacktrace:\n%s", a.getName(), a.getId(), sm.toString()));
                });
            }
        }, 0, -4000);
    }
}

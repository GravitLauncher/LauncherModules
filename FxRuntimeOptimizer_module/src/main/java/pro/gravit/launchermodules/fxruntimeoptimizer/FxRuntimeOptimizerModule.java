package pro.gravit.launchermodules.fxruntimeoptimizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launcher.base.modules.events.ClosePhase;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;

public class FxRuntimeOptimizerModule extends LauncherModule {
    private transient final Logger logger = LogManager.getLogger(FxRuntimeOptimizerModule.class);
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.STABLE);
    public RuntimeOptimizerHook hook;
    private transient MainBuildTask task;

    public FxRuntimeOptimizerModule() {
        super(new LauncherModuleInfoBuilder().setName("FxRuntimeOptimizer").setVersion(version).setDependencies(new String[]{"LaunchServerCore"}).createLauncherModuleInfo());
    }


    public void finish(LaunchServerPostInitPhase event) {
        installHooks(event.server);
    }

    public void installHooks(LaunchServer server) {
        hook = new RuntimeOptimizerHook(server, this);
        task = server.launcherBinary.getTaskByClass(MainBuildTask.class).orElseThrow();
        task.preBuildHook.registerHook(hook);
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerPostInitPhase.class);
        registerEvent(this::close, ClosePhase.class);
        if (initContext instanceof LaunchServerInitContext ctx) {
            installHooks(ctx.server);
        }
    }

    public void close(ClosePhase closePhase) {
        if(task != null && hook != null) {
            task.preBuildHook.unregisterHook(hook);
        }
    }
}

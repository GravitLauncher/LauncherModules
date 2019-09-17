package pro.gravit.launchermodules.statistics;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.utils.Version;

public class ServerStatisticsModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 0, Version.Type.LTS);
    public transient LaunchServer server;
    public final StatisticsManager manager;

    public ServerStatisticsModule() {
    	super(new LauncherModuleInfo("ServerStatistics", version));
        manager = new StatisticsManager();
    }

    public void postInit(LaunchServerFullInitEvent context) {
        server = context.server;
        manager.loadTime = System.currentTimeMillis();
        server.authHookManager.checkServerHook.registerHook((response, client) -> {
            manager.checkServerNumber++;
            return false;
        });
        server.authHookManager.joinServerHook.registerHook((response, client) -> {
            manager.joinServerNumber++;
            return false;
        });
        server.authHookManager.preHook.registerHook((con, client) -> {
            manager.authNumber++;
            return false;
        });
        server.authHookManager.postHook.registerHook((con, client) -> {
            manager.fullAuthNumber++;
            return false;
        });
        server.nettyServerSocketHandler.nettyServer.pipelineHook.registerHook((con, ch) -> {
            manager.connectionNumber++;
            return false;
        });
        server.commandHandler.registerCommand("stat", new StatCommand(manager));
    }

	@Override
	public void init(LauncherInitContext initContext) {
        registerEvent(this::postInit, LaunchServerFullInitEvent.class);
	}
}

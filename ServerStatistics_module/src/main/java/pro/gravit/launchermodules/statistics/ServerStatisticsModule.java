package pro.gravit.launchermodules.statistics;

import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.Version;

public class ServerStatisticsModule implements Module {
    public static Version version = new Version(1,0,0);
    public transient LaunchServer server;
    public final StatisticsManager manager;

    public ServerStatisticsModule() {
        manager = new StatisticsManager();
    }

    @Override
    public String getName() {
        return "ServerStatistics";
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void init(ModuleContext context) {

    }

    @Override
    public void postInit(ModuleContext context) {
        LaunchServerModuleContext context1 = (LaunchServerModuleContext) context;
        server = context1.launchServer;
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
    public void preInit(ModuleContext context) {

    }

    @Override
    public void close() throws Exception {

    }
}

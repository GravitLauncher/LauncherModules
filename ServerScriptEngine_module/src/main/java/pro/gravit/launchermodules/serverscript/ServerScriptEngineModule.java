package pro.gravit.launchermodules.serverscript;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.Version.Type;

public class ServerScriptEngineModule extends LauncherModule {
    public static final Version version = new Version(1, 1, 0, 1, Type.BETA);
    public static ServerScriptEngine scriptEngine;

    public ServerScriptEngineModule() {
        super(new LauncherModuleInfo("ServerScriptEngine", version, new String[]{"LaunchServerCore"}));
    }

    public void postInit(LaunchServerFullInitEvent event) {
        initScriptEngine(event.server);
    }

    public void initScriptEngine(LaunchServer server) {

        scriptEngine = new ServerScriptEngine();
        scriptEngine.initBaseBindings(server);
        server.commandHandler.registerCommand("eval", new EvalCommand(server));
        server.commandHandler.registerCommand("scriptMappings", new ScriptMappingsCommand(server));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::postInit, LaunchServerFullInitEvent.class);
        if (initContext != null) {
            if (initContext instanceof LaunchServerInitContext) {
                initScriptEngine(((LaunchServerInitContext) initContext).server);
            }
        }
    }
}

package ru.gravit.launchermodules.serverscript;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;
import ru.gravit.utils.Version;

public class ServerScriptEngineModule implements Module {
    public static Version version = new Version(1,0,0);
    public static ServerScriptEngine scriptEngine;
    @Override
    public String getName() {
        return  "ServerScriptEngine";
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
    public void postInit(ModuleContext context1) {
        LaunchServerModuleContext context = (LaunchServerModuleContext) context1;
        scriptEngine = new ServerScriptEngine();
        scriptEngine.initBaseBindings();
        context.launchServer.commandHandler.registerCommand("eval", new EvalCommand(context.launchServer));
    }

    @Override
    public void preInit(ModuleContext context) {

    }

    @Override
    public void close() throws Exception {

    }
}

package pro.gravit.launchermodules.serverscript;

import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.Version;

public class ServerScriptEngineModule implements Module {
    public static Version version = new Version(1, 1, 0);
    public static ServerScriptEngine scriptEngine;

    @Override
    public String getName() {
        return "ServerScriptEngine";
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
        context.launchServer.commandHandler.registerCommand("scriptMappings", new ScriptMappingsCommand(context.launchServer));
    }

    @Override
    public void preInit(ModuleContext context) {

    }

    @Override
    public void close() throws Exception {

    }
}

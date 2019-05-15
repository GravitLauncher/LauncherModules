package ru.gravit.launchermodules.unsafecommands;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;
import ru.gravit.utils.Version;

public class UnsafeCommandsModule implements Module {
    public static Version version = new Version(1, 0, 0);

    @Override
    public String getName() {
        return "UnsafeCommands";
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
        context1.launchServer.commandHandler.registerCommand("loadJar", new LoadJarCommand(context1.launchServer));
        context1.launchServer.commandHandler.registerCommand("registerComponent", new RegisterComponentCommand(context1.launchServer));
    }

    @Override
    public void preInit(ModuleContext context) {

    }

    @Override
    public void close() throws Exception {

    }
}

package pro.gravit.launchermodules.unsafecommands;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.command.BaseCommandCategory;
import pro.gravit.utils.command.CommandHandler;

public class UnsafeCommandsModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.EXPERIMENTAL);

    public UnsafeCommandsModule() {
        super(new LauncherModuleInfo("UnsafeCommands", version));
    }

    public void init(LaunchServerInitPhase initPhase) {
        BaseCommandCategory category = new BaseCommandCategory();
        category.registerCommand("loadJar", new LoadJarCommand(initPhase.server));
        category.registerCommand("registerComponent", new RegisterComponentCommand(initPhase.server));
        category.registerCommand("setSecurityManager", new SetSystemSecurityManagerCommand(initPhase.server));
        category.registerCommand("newDownloadAsset", new NewDownloadAssetCommand(initPhase.server));
        category.registerCommand("newDownloadClient", new FetchClientCommand(initPhase.server));
        category.registerCommand("sendAuth", new SendAuthCommand(initPhase.server));
        category.registerCommand("patcher", new PatcherCommand(initPhase.server));
        initPhase.server.commandHandler.registerCategory(new CommandHandler.Category(category, "Unsafe"));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::init, LaunchServerInitPhase.class);
        if (initContext != null) {
            if (initContext instanceof LaunchServerInitContext) {
                init(new LaunchServerInitPhase(((LaunchServerInitContext) initContext).server));
            }
        }
    }
}

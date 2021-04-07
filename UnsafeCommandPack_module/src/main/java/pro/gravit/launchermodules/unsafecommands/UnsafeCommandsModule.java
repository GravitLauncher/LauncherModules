package pro.gravit.launchermodules.unsafecommands;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchermodules.unsafecommands.commands.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.command.BaseCommandCategory;
import pro.gravit.utils.command.CommandHandler;

public class UnsafeCommandsModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.EXPERIMENTAL);

    public UnsafeCommandsModule() {
        super(new LauncherModuleInfo("UnsafeCommands", version, new String[]{"LaunchServerCore"}));
    }

    public void init(LaunchServer server) {
        BaseCommandCategory category = new BaseCommandCategory();
        category.registerCommand("loadJar", new LoadJarCommand(server));
        category.registerCommand("registerComponent", new RegisterComponentCommand(server));
        category.registerCommand("setSecurityManager", new SetSystemSecurityManagerCommand(server));
        category.registerCommand("newDownloadAsset", new NewDownloadAssetCommand(server));
        category.registerCommand("newDownloadClient", new FetchClientCommand(server));
        category.registerCommand("sendAuth", new SendAuthCommand(server));
        category.registerCommand("patcher", new PatcherCommand(server));
        category.registerCommand("cipherList", new CipherListCommand(server));
        server.commandHandler.registerCategory(new CommandHandler.Category(category, "Unsafe"));
    }

    public void initPhase(LaunchServerInitPhase initPhase) {
        init(initPhase.server);
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::initPhase, LaunchServerInitPhase.class);
        if (initContext != null) {
            if (initContext instanceof LaunchServerInitContext) {
                init(((LaunchServerInitContext) initContext).server);
            }
        }
    }
}

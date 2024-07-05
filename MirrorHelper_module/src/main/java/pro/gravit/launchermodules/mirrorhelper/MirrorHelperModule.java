package pro.gravit.launchermodules.mirrorhelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.config.JsonConfigurable;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.launchermodules.mirrorhelper.commands.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.command.BaseCommandCategory;
import pro.gravit.utils.command.CommandCategory;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MirrorHelperModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    private final Logger logger = LogManager.getLogger();
    public Config config;
    public JsonConfigurable<Config> configurable;

    public MirrorHelperModule() {
        super(new LauncherModuleInfo("MirrorHelper", version, new String[]{"LaunchServerCore"}));
    }

    public Path getWorkspaceDir() {
        return getConfigDir().resolve("workspace");
    }

    public Path getConfigDir() {
        return modulesConfigManager.getModuleConfigDir(moduleInfo.name);
    }

    public MirrorWorkspace getWorkspace() {
        if(config.workspace == null) {
            if(config.workspaceFile == null) {
                return null;
            }
            try(Reader reader = IOHelper.newReader(Paths.get(config.workspaceFile))) {
                config.workspace = Launcher.gsonManager.gson.fromJson(reader, MirrorWorkspace.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return config.workspace;
    }


    public void finish(LaunchServerPostInitPhase event) {
        initialize(event.server);
    }

    public void initialize(LaunchServer server) {
        MirrorHelperModule module = this;
        configurable = new JsonConfigurable<>(Config.class, modulesConfigManager.getModuleConfig(moduleInfo.name)) {
            @Override
            public Config getConfig() {
                return config;
            }

            @Override
            public void setConfig(Config config) {
                module.config = config;
            }

            @Override
            public Config getDefaultConfig() {
                return new Config();
            }
        };
        try {
            configurable.loadConfig();
        } catch (IOException e) {
            logger.error(e);
            config = configurable.getDefaultConfig();
        }
        CommandCategory commands = new BaseCommandCategory();
        commands.registerCommand("curseforge", new CurseforgeCommand(server, config));
        commands.registerCommand("installClient", new InstallClientCommand(server, this));
        commands.registerCommand("installMods", new InstallModCommand(server, this));
        commands.registerCommand("deduplibraries", new DeDupLibrariesCommand(server));
        commands.registerCommand("launchInstaller", new LaunchInstallerCommand(server));
        commands.registerCommand("lwjgldownload", new LwjglDownloadCommand(server));
        commands.registerCommand("patchauthlib", new PatchAuthlibCommand(server));
        commands.registerCommand("applyworkspace", new ApplyWorkspaceCommand(server, this));
        CommandHandler.Category category = new CommandHandler.Category(commands, "mirror");
        server.commandHandler.registerCategory(category);
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerPostInitPhase.class);
        if (initContext instanceof LaunchServerInitContext launchServerInitContext) {
            initialize(launchServerInitContext.server);
        }
    }
}

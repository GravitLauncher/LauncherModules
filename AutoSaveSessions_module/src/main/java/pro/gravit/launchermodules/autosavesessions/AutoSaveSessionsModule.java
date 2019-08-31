package pro.gravit.launchermodules.autosavesessions;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.reflect.TypeToken;

import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.ClosePhase;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.events.LaunchServerInitPhase;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class AutoSaveSessionsModule extends LauncherModule {
    public static String FILENAME = "sessions.json";
    public static boolean isClearSessionsBeforeSave = true;
    public Path file;
	private LaunchServer srv;

    public AutoSaveSessionsModule() {
        super(new LauncherModuleInfo("AutoSaveSessions", new Version(1,1,0)));
    }

    public void close(ClosePhase closePhase) {
        if (isClearSessionsBeforeSave) {
        	srv.sessionManager.garbageCollection();
        }
        Set<Client> clientSet = srv.sessionManager.getSessions();
        try (Writer writer = IOHelper.newWriter(file)) {
            LogHelper.info("Write sessions to %s", FILENAME);
            Launcher.gsonManager.configGson.toJson(clientSet, writer);
            LogHelper.info("%d sessions writed", clientSet.size());
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::init, LaunchServerInitPhase.class);
        registerEvent(this::postInit, LaunchServerFullInitEvent.class);
        registerEvent(this::close, ClosePhase.class);
        if(initContext != null)
        {
            if(initContext instanceof LaunchServerInitContext)
            {
                srv = ((LaunchServerInitContext) initContext).server;
                postInit(null);
            }
        }
    }

    public void init(LaunchServerInitPhase initPhase)
    {
        srv = initPhase.server;
    }

    public void postInit(LaunchServerFullInitEvent postInitPhase)
    {
        Path configDir = modulesConfigManager.getModuleConfigDir("AutoSaveSessions");
        if (!IOHelper.isDir(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        file = configDir.resolve(FILENAME);
        if (IOHelper.exists(file)) {
            LogHelper.info("Load sessions from %s", FILENAME);
            Type setType = new TypeToken<HashSet<Client>>() {
            }.getType();
            try (Reader reader = IOHelper.newReader(file)) {
                Set<Client> clientSet = Launcher.gsonManager.configGson.fromJson(reader, setType);
                for (Client client : clientSet) {
                    if (client.isAuth) client.updateAuth(srv);
                }
                srv.sessionManager.loadSessions(clientSet);
                LogHelper.info("Loaded %d sessions", clientSet.size());
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
    }
}

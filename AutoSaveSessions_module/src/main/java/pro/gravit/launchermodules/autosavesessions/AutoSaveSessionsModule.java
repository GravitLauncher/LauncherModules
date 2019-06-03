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
import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class AutoSaveSessionsModule implements Module {
    public static Version version = new Version(1, 0, 0);
    public static String FILENAME = "sessions.json";
    public static boolean isClearSessionsBeforeSave = true;
    public Path file;
	private LaunchServer srv;

    @Override
    public String getName() {
        return "AutoSaveSessions";
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
        Path configDir = context.modulesConfigManager.getModuleConfigDir(getName());
        if (!IOHelper.isDir(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        srv = context.launchServer;
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
                context.launchServer.sessionManager.loadSessions(clientSet);
                LogHelper.info("Loaded %d sessions", clientSet.size());
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
    }

    @Override
    public void preInit(ModuleContext context) {

    }

    @Override
    public void close() {
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
}

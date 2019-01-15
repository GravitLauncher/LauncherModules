package ru.gravit.launchermodules.autosavesessions;

import com.google.gson.reflect.TypeToken;
import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;
import ru.gravit.launchserver.socket.Client;
import ru.gravit.utils.Version;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class AutoSaveSessionsModule implements Module {
    public static Version version = new Version(1, 0, 0);
    public static String FILENAME = "sessions.json";
    public static boolean isClearSessionsBeforeSave = true;
    public Path file;

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
        file = configDir.resolve(FILENAME);
        if (IOHelper.exists(file)) {
            LogHelper.info("Load sessions from %s", FILENAME);
            Type setType = new TypeToken<HashSet<Client>>() {
            }.getType();
            try (Reader reader = IOHelper.newReader(file)) {
                Set<Client> clientSet = LaunchServer.gson.fromJson(reader, setType);
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
            LaunchServer.server.sessionManager.garbageCollection();
        }
        Set<Client> clientSet = LaunchServer.server.sessionManager.getSessions();
        try (Writer writer = IOHelper.newWriter(file)) {
            LogHelper.info("Write sessions to %s", FILENAME);
            LaunchServer.gson.toJson(clientSet, writer);
            LogHelper.info("%d sessions writed", clientSet.size());
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }
}

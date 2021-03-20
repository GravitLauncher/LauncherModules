package pro.gravit.launchermodules.fileauthsystem.providers;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.auth.handler.CachedAuthHandler;

import java.io.IOException;
import java.util.UUID;

public class FileSystemAuthHandler extends CachedAuthHandler {
    @Override
    public void close() throws IOException {

    }

    @Override
    protected Entry fetchEntry(String username) throws IOException {
        FileAuthSystemModule module = srv.modulesManager.getModule(FileAuthSystemModule.class);
        FileAuthSystemModule.UserEntity entity = module.getUser(username);
        if (entity == null) return null;
        return new Entry(entity.uuid, entity.username, entity.accessToken, entity.serverId);
    }

    @Override
    protected Entry fetchEntry(UUID uuid) throws IOException {
        FileAuthSystemModule module = srv.modulesManager.getModule(FileAuthSystemModule.class);
        FileAuthSystemModule.UserEntity entity = module.getUser(uuid);
        if (entity == null) return null;
        return new Entry(entity.uuid, entity.username, entity.accessToken, entity.serverId);
    }

    @Override
    protected boolean updateAuth(UUID uuid, String username, String accessToken) throws IOException {
        FileAuthSystemModule module = srv.modulesManager.getModule(FileAuthSystemModule.class);
        FileAuthSystemModule.UserEntity entity = module.getUser(uuid);
        if (entity == null) return false;
        entity.accessToken = accessToken;
        return true;
    }

    @Override
    protected boolean updateServerID(UUID uuid, String serverID) throws IOException {
        FileAuthSystemModule module = srv.modulesManager.getModule(FileAuthSystemModule.class);
        FileAuthSystemModule.UserEntity entity = module.getUser(uuid);
        if (entity == null) return false;
        entity.serverId = serverID;
        return true;
    }
}

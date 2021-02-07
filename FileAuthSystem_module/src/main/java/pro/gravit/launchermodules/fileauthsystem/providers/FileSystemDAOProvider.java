package pro.gravit.launchermodules.fileauthsystem.providers;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.dao.provider.DaoProvider;

public class FileSystemDAOProvider extends DaoProvider {
    @Override
    public void init(LaunchServer server) {
        FileAuthSystemModule module = server.modulesManager.getModule(FileAuthSystemModule.class);
        this.userDAO = new FileSystemUserDAO(module);
    }
}

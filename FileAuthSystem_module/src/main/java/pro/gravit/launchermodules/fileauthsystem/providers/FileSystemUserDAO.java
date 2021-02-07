package pro.gravit.launchermodules.fileauthsystem.providers;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.dao.User;
import pro.gravit.launchserver.dao.UserDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FileSystemUserDAO implements UserDAO {
    private final FileAuthSystemModule module;

    public FileSystemUserDAO(FileAuthSystemModule module) {
        this.module = module;
    }

    @Override
    public User findById(int id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User findByUsername(String username) {
        return module.getUser(username);
    }

    @Override
    public User findByUUID(UUID uuid) {
        return module.getUser(uuid);
    }

    @Override
    public void save(User user) {
        module.addUser((FileAuthSystemModule.UserEntity) user);
    }

    @Override
    public void update(User user) {
        // None
    }

    @Override
    public void delete(User user) {
        module.deleteUser((FileAuthSystemModule.UserEntity) user);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(module.getAllUsers());
    }
}

package pro.gravit.launchermodules.fileauthsystem.providers;

import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;

import java.io.IOException;
import java.util.UUID;

public class FileSystemAuthCoreProvider extends AuthCoreProvider {
    private FileAuthSystemModule module;
    @Override
    public User getUserByUsername(String username) {
        return module.getUser(username);
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        return module.getUser(uuid);
    }

    @Override
    public void verifyAuth(AuthResponse.AuthContext context) throws AuthException {
        // None
    }

    @Override
    public PasswordVerifyReport verifyPassword(User user, AuthRequest.AuthPasswordInterface password) {
        FileAuthSystemModule.UserEntity entity = (FileAuthSystemModule.UserEntity) user;
        if (!(password instanceof AuthPlainPassword)) {
            return PasswordVerifyReport.FAILED;
        }
        AuthPlainPassword plainPassword = (AuthPlainPassword) password;
        if(entity.verifyPassword(plainPassword.password)) {
            return new PasswordVerifyReport(true);
        }
        return PasswordVerifyReport.FAILED;
    }

    @Override
    public void init(LaunchServer server) {
        module = server.modulesManager.getModule(FileAuthSystemModule.class);
    }

    @Override
    protected boolean updateAuth(User user) throws IOException {
        FileAuthSystemModule.UserEntity entity = (FileAuthSystemModule.UserEntity) user;
        if (entity == null) return false;
        entity.serverId = null;
        return true;
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        FileAuthSystemModule.UserEntity entity = (FileAuthSystemModule.UserEntity) user;
        if (entity == null) return false;
        entity.serverId = serverID;
        return true;
    }

    @Override
    public void close() throws IOException {

    }
}

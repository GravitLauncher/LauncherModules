package pro.gravit.launchermodules.fileauthsystem.providers;

import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;

public class FileSystemAuthProvider extends AuthProvider {
    public String errorMessage = "Login or password incorrect";

    @Override
    public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws Exception {
        if (!(password instanceof AuthPlainPassword)) {
            throw new AuthException("password type not supported");
        }
        String passwd = ((AuthPlainPassword) password).password;
        FileAuthSystemModule module = srv.modulesManager.getModule(FileAuthSystemModule.class);
        FileAuthSystemModule.UserEntity entity = module.getUser(login);
        if (entity == null || !entity.verifyPassword(passwd)) {
            throw new AuthException(errorMessage);
        }
        return new AuthProviderResult(entity.username, SecurityHelper.randomStringToken(), entity.permissions);
    }

    @Override
    public void close() throws IOException {

    }
}

package pro.gravit.launchermodules.addhash.verifier;

import org.mindrot.jbcrypt.BCrypt;
import pro.gravit.launchserver.auth.password.PasswordVerifier;

public class BCryptPasswordVerifier extends PasswordVerifier {
    @Override
    public boolean check(String encryptedPassword, String password) {
        return BCrypt.checkpw(password, "$2a" + encryptedPassword.substring(3));
    }

    @Override
    public String encrypt(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}

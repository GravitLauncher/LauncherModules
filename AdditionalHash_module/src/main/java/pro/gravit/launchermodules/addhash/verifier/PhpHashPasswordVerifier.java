package pro.gravit.launchermodules.addhash.verifier;

import com.github.wolf480pl.phpass.PHPass;
import pro.gravit.launchserver.auth.password.PasswordVerifier;

public class PhpHashPasswordVerifier extends PasswordVerifier {
    private transient final PHPass pass = new PHPass(8);

    @Override
    public boolean check(String encryptedPassword, String password) {
        return pass.checkPassword(password, encryptedPassword);
    }

    @Override
    public String encrypt(String password) {
        return pass.hashPassword(password);
    }
}

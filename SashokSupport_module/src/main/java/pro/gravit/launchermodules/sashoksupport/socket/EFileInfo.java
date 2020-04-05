package pro.gravit.launchermodules.sashoksupport.socket;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class EFileInfo {
    private static final String RSA_ALGO = "RSA";
    private static final String RSA_SIGN_ALGO = "SHA256withRSA";
    public final Path path;
    public final int len;
    public final byte[] digest;
    public final byte[] sign;

    public EFileInfo(String path, Path key) throws IOException, InvalidKeySpecException {
        this.path = Paths.get(path);
        len = (int) Files.size(this.path);
        digest = SecurityHelper.digest(SecurityHelper.DigestAlgorithm.SHA512, path);
        sign = sign(IOHelper.read(this.path), toPrivateRSAKey(IOHelper.read(key)));
    }

    private static byte[] sign(byte[] bytes, RSAPrivateKey privateKey) {
        Signature signature = newRSASignSignature(privateKey);
        try {
            signature.update(bytes);
            return signature.sign();
        } catch (SignatureException e) {
            throw new InternalError(e);
        }
    }

    private static RSAPrivateKey toPrivateRSAKey(byte[] bytes) throws InvalidKeySpecException {
        return (RSAPrivateKey) newRSAKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private static KeyFactory newRSAKeyFactory() {
        try {
            return KeyFactory.getInstance(RSA_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    private static Signature newRSASignature() {
        try {
            return Signature.getInstance(RSA_SIGN_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError(e);
        }
    }

    private static Signature newRSASignSignature(RSAPrivateKey key) {
        Signature signature = newRSASignature();
        try {
            signature.initSign(key);
        } catch (InvalidKeyException e) {
            throw new InternalError(e);
        }
        return signature;
    }

}

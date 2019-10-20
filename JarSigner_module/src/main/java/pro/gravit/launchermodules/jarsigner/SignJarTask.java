package pro.gravit.launchermodules.jarsigner;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.operator.OperatorCreationException;

public class SignJarTask implements LauncherBuildTask {

    private final ModuleImpl impl;
    private final LaunchServer srv;

    public SignJarTask(LaunchServer srv, ModuleImpl impl) {
        this.srv = srv;
        this.impl = impl;
    }

    @Override
    public String getName() {
        return "SignJar";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path toRet = srv.launcherBinary.nextPath("signed");
        KeyStore c = SignHelper.getStore(new File(impl.config.key).toPath(), impl.config.storepass, impl.config.algo);
        try (SignerJar output = new SignerJar(new ZipOutputStream(IOHelper.newOutput(toRet)), () -> this.gen(c));
             ZipInputStream input = new ZipInputStream(IOHelper.newInput(inputFile))) {
            //input.getManifest().getMainAttributes().forEach((a, b) -> output.addManifestAttribute(a.toString(), b.toString())); // may not work such as after Radon.
            ZipEntry e = input.getNextEntry();
            while (e != null) {
               if ("META-INF/MANIFEST.MF".equals(e.getName()) || "/META-INF/MANIFEST.MF".equals(e.getName())) {
               	 Manifest m = new Manifest(input);
               	 m.getMainAttributes().forEach((a, b) -> output.addManifestAttribute(a.toString(), b.toString()));
               	 e = input.getNextEntry();
               	 continue;
               }
                output.addFileContents(IOHelper.newZipEntry(e), input);
                e = input.getNextEntry();
            }
        }
        return toRet;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }
    
    public CMSSignedDataGenerator gen(KeyStore c) {
    	try {
			return SignHelper.createSignedDataGenerator(c,
			        impl.config.keyalias, impl.config.signAlgo, impl.config.pass);
		} catch (CertificateEncodingException | UnrecoverableKeyException | KeyStoreException
				| OperatorCreationException | NoSuchAlgorithmException | CMSException e) {
			LogHelper.error(e);
			return null;
		}
    }
}

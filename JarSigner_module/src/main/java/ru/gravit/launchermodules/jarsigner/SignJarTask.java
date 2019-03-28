package ru.gravit.launchermodules.jarsigner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.binary.tasks.LauncherBuildTask;
import ru.gravit.utils.helper.IOHelper;

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
		try (SignerJar output = new SignerJar(IOHelper.newOutput(toRet),
                SignerJar.getStore(new File(impl.config.key).toPath(), impl.config.storepass, impl.config.algo),
                impl.config.keyalias, impl.config.pass);
             JarInputStream input = new JarInputStream(IOHelper.newInput(inputFile))) {
            ZipEntry e = input.getNextEntry();
            while (e != null) {
            	if ("META-INF/MANIFEST.MF".equals(e.getName())) continue;
                output.addFileContents(e, input);
                e = input.getNextEntry();
            }
            input.getManifest().getMainAttributes().forEach((a, b) -> {
            	output.addManifestAttribute(a.toString(), b.toString());
            });
        }
		return toRet;
	}

	@Override
	public boolean allowDelete() {
		return true;
	}
}

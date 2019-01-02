package ru.gravit.launcher.addhash;

import ru.gravit.utils.Version;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.auth.provider.AuthProvider;

public class AddHashModule implements Module {
	private static boolean registred = false;
	@Override
	public void close() {

	}

	@Override
	public String getName() {
		return "AddHash";
	}

	@Override
	public Version getVersion() {
		return new Version(1, 0, 2, 0);
	}

	@Override
	public int getPriority() {
		return 1;
	}

	@Override
	public void init(ModuleContext context1) {
	}

	@Override
	public void preInit(ModuleContext context1) {
        if (!registred) {
            AuthProvider.registerProvider("mysql-bcrypt", MySQLBcryptAuthProvider.class);
            registred = true;
        }
	}

	@Override
	public void postInit(ModuleContext context1) {
		
	}
}

package kz.sasha0552.launchermodules.autorehash;

import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.*;

public class AutoReHashModule implements Module {
    public static Version version = new Version(1, 0, 0);
    private WatchService watchService;

    @Override
    public String getName() {
        return "AutoReHashModule";
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void preInit(ModuleContext context1) {
        LaunchServerModuleContext context = (LaunchServerModuleContext) context1;
        Path updates = Paths.get(context.launchServer.dir.toString(), "updates");
        try {
            watchService = FileSystems.getDefault().newWatchService();
            updates.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            LogHelper.error(e.toString());
        }
	}

    @Override
    public void init(ModuleContext context1) {
        LaunchServerModuleContext context = (LaunchServerModuleContext) context1;
		Runnable task = () -> {
			try {
				while (true) {
					WatchKey key = watchService.take();
					for (WatchEvent<?> event : key.pollEvents()) {
						LogHelper.debug("Rehashing updates for you");
						context.launchServer.syncUpdatesDir(null);
					}
					key.reset();
				}
			} catch (InterruptedException | IOException e) {
				LogHelper.error(e.toString());
			}
		};
		Thread thread = new Thread(task);
		thread.start();
    }

    @Override
    public void postInit(ModuleContext context) {

    }

    @Override
    public void close()  {

    }
}

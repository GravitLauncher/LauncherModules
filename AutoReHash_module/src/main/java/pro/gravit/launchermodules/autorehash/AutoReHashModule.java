package pro.gravit.launchermodules.autorehash;

import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launchserver.modules.LaunchServerModuleContext;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.*;
import java.util.Timer;
import java.util.TimerTask;

import static pro.gravit.utils.helper.CommonHelper.newThread;

public class AutoReHashModule implements Module {
    public static Version version = new Version(1, 0, 0);
    private WatchService watchService;
    private volatile boolean changed = false;
	private Timer timer;

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
                    	LogHelper.info("Changed " + (event.context() != null ? event.context().toString() : "unknown") + " kind " + event.kind().name());
                    	changed = true;
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                LogHelper.error(e.toString());
            }
        };
        Thread thread = newThread("ReHashing", true, task);
        thread.start();
        timer = new Timer(true);
        timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if (changed) {
					try {
						context.launchServer.syncUpdatesDir(null);
					} catch (IOException e) {
						LogHelper.error(e);
					}
					changed = false;
				}
			}
        	
        }, 30000, 30000);
    }

    @Override
    public void postInit(ModuleContext context) {

    }

    @Override
    public void close() {

    }
}
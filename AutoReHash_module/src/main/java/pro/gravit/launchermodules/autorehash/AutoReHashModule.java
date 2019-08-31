package pro.gravit.launchermodules.autorehash;

import static pro.gravit.utils.helper.CommonHelper.newThread;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Timer;
import java.util.TimerTask;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

public class AutoReHashModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0);
    private WatchService watchService;
    private volatile boolean changed = false;
	private Timer timer;

	public AutoReHashModule() {
        super(new LauncherModuleInfo("AutoReHashModule", version));
	}

	@Override
	public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerPostInitPhase.class);
	}
	public void finish(LaunchServerPostInitPhase context) {
        Path updates = Paths.get(context.server.dir.toString(), "updates");
        try {
            watchService = FileSystems.getDefault().newWatchService();
            updates.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (IOException e) {
            LogHelper.error(e);
        }
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
						context.server.syncUpdatesDir(null);
					} catch (IOException e) {
						LogHelper.error(e);
					}
					changed = false;
				}
			}
        }, 30000, 30000);
	}
}
package pro.gravit.launchermodules.autorehash;

import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launchserver.modules.events.LaunchServerPostInitPhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static pro.gravit.utils.helper.CommonHelper.newThread;

public class AutoReHashModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.STABLE);
    private final Set<String> dirs = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private WatchService watchService;
    private volatile boolean changed = false;
    private Timer timer;

    public AutoReHashModule() {
        super(new LauncherModuleInfo("AutoReHashModule", version));
    }

    private static Deque<String> toPath(Iterable<Path> path) {
        Deque<String> result = new LinkedList<>();
        for (Path pe : path)
            result.add(pe.toString());
        return result;
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::finish, LaunchServerPostInitPhase.class);
    }

    public void finish(LaunchServerPostInitPhase context) {
        Path updates = context.server.updatesDir;
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
                while (!Thread.interrupted()) {
                    WatchKey key = watchService.take();
                    Path watchDir = (Path) key.watchable();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind().equals(StandardWatchEventKinds.OVERFLOW))
                            continue;
                        Path path = watchDir.resolve((Path) event.context());
                        LogHelper.info("Changed " + updates.relativize(path) + " kind " + event.kind().name());
                        String stringPath = toPath(updates.relativize(path)).peekFirst();
                        if (stringPath != null) {
                            LogHelper.debug("To sync (may be file): " + stringPath);
                            if (Files.isDirectory(updates.resolve(stringPath))) {
                                dirs.add(stringPath);
                                changed = true;
                            }
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                return;
            }
        };
        Thread thread = newThread("ReHashing", true, task);
        thread.start();
        timer = new Timer("ReHashTimer", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (changed) {
                    try {
                        context.server.syncUpdatesDir(dirs);
                    } catch (IOException e) {
                        LogHelper.error(e);
                    }
                    dirs.clear();
                    changed = false;
                }
            }
        }, 30000, 30000);
    }
}
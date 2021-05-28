package pro.gravit.launchermodules.onelauncher;

import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.client.ClientProcessLaunchEvent;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.ClosePhase;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import javax.swing.*;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class OneLauncherModule extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    private final OneLauncherConfig config = new OneLauncherConfig();
    private Path path;
    private Path locksDir;
    private FileLock lock;
    private FileChannel channel;

    public OneLauncherModule() {
        super(new LauncherModuleInfo("OneLauncher", version, -9999, new String[]{"ClientLauncherCore"}));
    }

    public void prepareLock(String name) {
        locksDir = DirBridge.dir.resolve("locks");
        if (!IOHelper.isDir(locksDir)) {
            try {
                Files.createDirectories(locksDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        path = locksDir.resolve(name + ".lock");
        if (!Files.exists(path)) {
            try {
                IOHelper.write(path, new byte[]{0, 1, 2, 3, 4, 5} /* any bytes */);
            } catch (IOException e) {
                LogHelper.error(e);
                startError();
            }
        }
    }

    public boolean tryLock(Path path, boolean hold, OpenOption... options) {
        if (!Files.exists(path)) return false;
        try {
            channel = FileChannel.open(path, options);
            lock = channel.tryLock(); // Exclusive lock
            if (lock == null || !lock.isValid()) {
                return false;
            }
            if (!hold) {
                lock.close();
                lock = null;
            }
        } catch (IOException e) {
            LogHelper.error(e);
            return false;
        }
        return true;
    }

    public void initClient(ClientProcessLaunchEvent event) {
        if (lock != null) return;
        prepareLock(config.multipleProfilesAllow ? event.params.profile.getUUID().toString() : "client");
        if (!tryLock(path, true, StandardOpenOption.WRITE)) {
            event.cancel();
            startError();
        }
    }

    public void initLauncher(ClientEngineInitPhase phase) {
        if (lock != null) return;
        prepareLock("launcher");
        if (!config.multipleProfilesAllow && config.checkClientLock) {
            if (!tryLock(locksDir.resolve("client.lock"), false, StandardOpenOption.WRITE)) {
                phase.cancel();
                startError();
            }
        }
        if (!tryLock(path, true, StandardOpenOption.WRITE)) {
            phase.cancel();
            startError();
        }
    }

    public void startError() {
        JOptionPane.showMessageDialog(null, config.text);
        LauncherEngine.exitLauncher(0);
    }

    public void closePhase(ClosePhase closePhase) {
        if (lock != null) {
            try {
                lock.close();
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if (config.clientLock) {
            registerEvent(this::initClient, ClientProcessLaunchEvent.class);
        }
        if (config.launcherLock) {
            registerEvent(this::initLauncher, ClientEngineInitPhase.class);
        }
        registerEvent(this::closePhase, ClosePhase.class);
    }
}

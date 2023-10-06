package pro.gravit.launchermodules.discordgame;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.GameSDKException;
import de.jcm.discordgamesdk.LogLevel;
import de.jcm.discordgamesdk.activity.Activity;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.api.DiscordActivityService;
import pro.gravit.launchermodules.discordgame.event.DiscordInitEvent;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.nio.file.Path;

public class DiscordBridge {
    public static final DiscordActivityService activityService = new DiscordActivityService();
    private static Thread thread;
    private static Core core;

    private static CreateParams params;
    private static Activity activity;

    private static void initCore() throws IOException {
    }

    public static void init(long appId) throws IOException {
        if (JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM32 || JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64) {
            LogHelper.info("Cannot initialize Discord Game SDK because of launcher started at unsupported system && arch");
            return;
        }
        initCore();
        params = new CreateParams();
        params.setClientID(appId);
        //params.setClientID(698611073133051974L);
        params.setFlags(CreateParams.getDefaultFlags() | 1);
        // Create the Core
        // here we would like to deal with
        try {
            core = new Core(params);
            core.setLogHook(getLogLevel(), (level, s) -> {
                switch (level) {
                    case ERROR -> {
                        LogHelper.error(s);
                    }
                    case WARN -> {
                        LogHelper.warning(s);
                    }
                    case INFO -> {
                        LogHelper.info(s);
                    }
                    case DEBUG -> {
                        LogHelper.debug(s);
                    }
                    case VERBOSE -> {
                        LogHelper.dev(s);
                    }
                }
            });
            {
                // Create the Activity
                activity = new Activity();
                activityService.applyToActivity(activity);
                activityService.resetStartTime();
                core.activityManager().updateActivity(DiscordBridge.getActivity());
            }
        } catch (GameSDKException e) {
            LogHelper.info("Failed to start Discord Game SDK. Most surely because local discord app is down");
            close();
            return;
        }
        //params.close();
        LauncherEngine.modulesManager.invokeEvent(new DiscordInitEvent(core));
        LogHelper.debug("Initialized Discord Game. Application ID %d", appId);
        thread = CommonHelper.newThread("DiscordGameBridge callbacks", true, new DiscordUpdateTask(core));
        thread.start();
    }

    private static LogLevel getLogLevel() {
        if(LogHelper.isDevEnabled()) {
            return LogLevel.VERBOSE;
        }
        if(LogHelper.isDebugEnabled()) {
            return LogLevel.INFO;
        }
        return LogLevel.ERROR;
    }

    public static Core getCore() {
        return core;
    }

    public static Activity getActivity() {
        return activity;
    }

    public static void close() {
        if (thread != null) {
            thread.interrupt();
        }

        if (core != null) {
            try {
                core.close();
            } catch (Throwable e) {
                if (LogHelper.isDebugEnabled()) {
                    LogHelper.error(e);
                }
                LogHelper.warning("DiscordGame core object not closed correctly. Discord is down?");
            }
        }

    }
    private static String loadNative(Path baseDir, String name, String osFolder, String arch) throws IOException {
        String nativeLib = JVMHelper.NATIVE_PREFIX.concat(name).concat(JVMHelper.NATIVE_EXTENSION);
        Path pathToLib = baseDir.resolve(nativeLib);
        String libraryPath = String.join(IOHelper.CROSS_SEPARATOR, "native", osFolder, arch, nativeLib);
        UnpackHelper.unpack(IOHelper.getResourceURL(libraryPath), pathToLib);
        System.load(pathToLib.toAbsolutePath().toString());
        return pathToLib.toAbsolutePath().toString();
    }
}

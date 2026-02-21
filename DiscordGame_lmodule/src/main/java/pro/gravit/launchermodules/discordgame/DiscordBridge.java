package pro.gravit.launchermodules.discordgame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.GameSDKException;
import de.jcm.discordgamesdk.LogLevel;
import de.jcm.discordgamesdk.activity.Activity;
import pro.gravit.launcher.client.api.DiscordActivityService;
import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.client.ClientLauncherEntryPoint;
import pro.gravit.launchermodules.discordgame.event.DiscordInitEvent;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.nio.file.Path;

public class DiscordBridge {

    private static final Logger logger =
            LoggerFactory.getLogger(DiscordBridge.class);

    public static final DiscordActivityService activityService = new DiscordActivityService();
    private static Thread thread;
    private static Core core;

    private static CreateParams params;
    private static Activity activity;

    private static void initCore() {
    }

    public static void init(long appId, boolean isClient) throws IOException {
        if (JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM32 || JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64) {
            logger.info("Cannot initialize Discord Game SDK because of launcher started at unsupported system && arch");
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
                    case ERROR -> logger.error("{}", s);
                    case WARN -> logger.warn("{}", s);
                    case INFO, VERBOSE -> logger.info("{}", s);
                    case DEBUG -> logger.debug("{}", s);
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
            logger.info("Failed to start Discord Game SDK. Most surely because local discord app is down");
            close();
            return;
        }
        //params.close();
        if(isClient) {
            ClientLauncherEntryPoint.modulesManager.invokeEvent(new DiscordInitEvent(core));
        } else {
            LauncherEngine.modulesManager.invokeEvent(new DiscordInitEvent(core));
        }
        logger.debug("Initialized Discord Game. Application ID {}", appId);
        thread = CommonHelper.newThread("DiscordGameBridge callbacks", true, new DiscordUpdateTask(core));
        thread.start();
    }

    private static LogLevel getLogLevel() {
        if(true) {
            return LogLevel.VERBOSE;
        }
        if(true) {
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
                if (true) {
                    logger.error("", e);
                }
                logger.warn("DiscordGame core object not closed correctly. Discord is down?");
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
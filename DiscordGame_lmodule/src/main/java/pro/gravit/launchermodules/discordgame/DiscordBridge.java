package pro.gravit.launchermodules.discordgame;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.GameSDKException;
import de.jcm.discordgamesdk.activity.Activity;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.api.DiscordActivityService;
import pro.gravit.launchermodules.discordgame.event.DiscordInitEvent;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class DiscordBridge {
    public static final DiscordActivityService activityService = new DiscordActivityService();
    private static Thread thread;
    private static Core core;

    private static CreateParams params;
    private static Activity activity;

    private static void initCore() throws IOException {
        Path baseDir = DirBridge.getGuardDir(JVMHelper.ARCH_TYPE, JVMHelper.OS_TYPE);

        String osFolder = "";
        switch (JVMHelper.OS_TYPE) {
            case MUSTDIE:
                osFolder = "windows";
                break;
            case LINUX:
                osFolder = "linux";
                break;
            case MACOSX:
                osFolder = "macos";
                break;
        }

        String arch;
        if (JVMHelper.ARCH_TYPE == JVMHelper.ARCH.X86_64) {
            arch = "amd64";
        } else {
            arch = JVMHelper.ARCH_TYPE.name;
        }
        String pathToSdkLib = DiscordBridge.loadNative(baseDir, "discord_game_sdk", osFolder, arch);
        DiscordBridge.loadNative(baseDir, "discord_game_sdk_jni", osFolder, arch);
        Core.initDiscordNative(pathToSdkLib);

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

        if (params != null) {
            try {
                params.close();
            } catch (Throwable e) {
                if (LogHelper.isDebugEnabled()) {
                    LogHelper.error(e);
                }
                LogHelper.warning("DiscordGame params object not closed correctly. Discord is down?");
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

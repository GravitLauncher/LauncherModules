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

public class DiscordBridge {
    public static final DiscordActivityService activityService = new DiscordActivityService();
    private static Thread thread;
    private static Core core;

    private static CreateParams params;
    private static Activity activity;

    private static void initCore() throws IOException {
        Path baseDir = DirBridge.getGuardDir(JVMHelper.ARCH_TYPE, JVMHelper.OS_TYPE);
        Path pathToLib;
        String arch = System.getProperty("os.arch");
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            pathToLib = baseDir.resolve("discord_game_sdk_jni.dll");
            String libraryJarPath = "native/windows/" + arch + "/discord_game_sdk_jni.dll";
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryJarPath), pathToLib);
        } else if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            pathToLib = baseDir.resolve("libdiscord_game_sdk_jni.so");
            String libraryJarPath = "native/linux/" + arch + "/libdiscord_game_sdk_jni.so";
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryJarPath), pathToLib);
        } else {
            pathToLib = baseDir.resolve("libdiscord_game_sdk_jni.dylib");
            String libraryJarPath = "native/macos/" + arch + "/libdiscord_game_sdk_jni.dylib";
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryJarPath), pathToLib);
        }
        Path pathToDiscordSdkLib;
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
            pathToDiscordSdkLib = baseDir.resolve("discord_game_sdk.dll");
            String libraryJarPath = "native/windows/" + arch + "/discord_game_sdk.dll";
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryJarPath), pathToDiscordSdkLib);
        } else if (JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            pathToDiscordSdkLib = baseDir.resolve("discord_game_sdk.so");
            String libraryJarPath = "native/linux/" + arch + "/discord_game_sdk.so";
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryJarPath), pathToDiscordSdkLib);
        } else {
            pathToDiscordSdkLib = baseDir.resolve("discord_game_sdk.dylib");
            String libraryJarPath = "native/macos/" + arch + "/discord_game_sdk.dylib";
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryJarPath), pathToDiscordSdkLib);
        }
        System.load(pathToDiscordSdkLib.toAbsolutePath().toString());
        System.load(pathToLib.toAbsolutePath().toString());
        Core.initDiscordNative(pathToDiscordSdkLib.toAbsolutePath().toString());
    }

    public static void init(long appId) throws IOException {
        if (JVMHelper.OS_TYPE == JVMHelper.OS.MACOSX && (JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM32 || JVMHelper.ARCH_TYPE == JVMHelper.ARCH.ARM64)) {
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
        if (core == null || thread == null) return;
        thread.interrupt();
        try {
            core.close();
        } catch (Throwable e) {
            if (LogHelper.isDebugEnabled()) {
                LogHelper.error(e);
            }
            LogHelper.warning("DiscordGame core object not closed correctly. Discord is down?");
        }
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

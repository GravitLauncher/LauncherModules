package pro.gravit.launchermodules.discordgame;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.activity.Activity;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.api.DiscordActivityService;
import pro.gravit.launchermodules.discordgame.event.DiscordInitEvent;
import pro.gravit.utils.helper.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;

public class DiscordBridge {
    private static Thread thread;
    private static Core core;
    private static Activity activity;

    public static final DiscordActivityService activityService = new DiscordActivityService();

    private static void initCore() throws IOException {
        Path pathToLib;
        if(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
			String libraryName = "discord_game_sdk_jni_"+String.valueOf(JVMHelper.JVM_BITS)+".dll";
            pathToLib =  DirBridge.getGuardDir().resolve("discord_game_sdk_jni.dll");
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryName), pathToLib);
        }
        else if(JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
			String libraryName = "libdiscord_game_sdk_jni_"+String.valueOf(JVMHelper.JVM_BITS)+".so";
            pathToLib =  DirBridge.getGuardDir().resolve("libdiscord_game_sdk_jni.so");
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryName), pathToLib);
        }
        else {
            throw new IOException("MacOS not supported");
        }
        Path pathToDiscordSdkLib;
        if(JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE) {
			String libraryName = "discord_game_sdk_"+String.valueOf(JVMHelper.JVM_BITS)+".dll";
            pathToDiscordSdkLib =  DirBridge.getGuardDir().resolve("discord_game_sdk.dll");
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryName), pathToDiscordSdkLib);
        }
        else {
			String libraryName = "discord_game_sdk_"+String.valueOf(JVMHelper.JVM_BITS)+".so";
            pathToDiscordSdkLib =  DirBridge.getGuardDir().resolve("discord_game_sdk.so");
            UnpackHelper.unpack(IOHelper.getResourceURL(libraryName), pathToDiscordSdkLib);
        }
		System.load(pathToDiscordSdkLib.toAbsolutePath().toString());
		System.load(pathToLib.toAbsolutePath().toString());
        try {
            Method method = Core.class.getDeclaredMethod("initDiscordNative", String.class);
            method.setAccessible(true);
            method.invoke(null, pathToDiscordSdkLib.toAbsolutePath().toString());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void init(long appId) throws IOException {
        initCore();
        try(CreateParams params = new CreateParams())
        {
            params.setClientID(appId);
            //params.setClientID(698611073133051974L);
            params.setFlags(CreateParams.getDefaultFlags());
            // Create the Core
            core = new Core(params);
            {
                // Create the Activity
                activity = new Activity();
                activityService.applyToActivity(activity);
                activityService.resetStartTime();
                core.activityManager().updateActivity(DiscordBridge.getActivity());
            }
            LauncherEngine.modulesManager.invokeEvent(new DiscordInitEvent(core));
            LogHelper.debug("Initialized Discord Game. Application ID %d", appId);
        }
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
        if(core == null) return;
        thread.interrupt();
        core.close();
    }
}

package pro.gravit.launchermodules.discordrpc;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRichPresence;

public class DiscordRPC {
    public static club.minnced.discord.rpc.DiscordRPC lib;
    public static DiscordRichPresence presence;
    public static DiscordParametersReplacer parameters = new DiscordParametersReplacer();
    static Thread thr;

    static void onConfig(String appId, String firstLine, String secondLine, String largeKey, String smallKey, String largeText, String smallText) {
        lib = club.minnced.discord.rpc.DiscordRPC.INSTANCE;
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        lib.Discord_Initialize(appId, handlers, true, "");
        presence = new DiscordRichPresence();
        presence.startTimestamp = System.currentTimeMillis() / 1000;
        presence.details = parameters.replace(firstLine);
        presence.state = parameters.replace(secondLine);
        //Ниче не лишнее, так надо
        if (largeKey != null) {
            presence.largeImageKey = parameters.replace(largeKey).toLowerCase().replaceAll(" ", "_");
        }
        if (smallKey != null) {
            presence.smallImageKey = parameters.replace(smallKey).toLowerCase().replaceAll(" ", "_");
        }
        if (largeKey != null && largeText != null) {
            presence.largeImageText = parameters.replace(largeText);
        }
        if (smallKey != null && smallText != null) {
            presence.smallImageText = parameters.replace(smallText);
        }

        lib.Discord_UpdatePresence(presence);

        thr = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                lib.Discord_RunCallbacks();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (!ClientModule.isClosed(false))
                lib.Discord_Shutdown();
        }, "RPC");
        thr.setDaemon(true);
        thr.start();
    }

    public static void resetPresence() {
        if (presence != null) {
            lib.Discord_UpdatePresence(presence);
        }
    }
}

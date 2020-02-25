package pro.gravit.launchermodules.discordrpc;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.google.gson.Gson;
import pro.gravit.utils.helper.CommonHelper;

class DiscordRPC {
    static final Gson confGson = CommonHelper.newBuilder().setPrettyPrinting().serializeNulls().create();
	static club.minnced.discord.rpc.DiscordRPC lib;
    static Thread thr;
    static DiscordRichPresence presence;

    static void onConfig(String appId, String firstLine, String secondLine, String largeKey, String smallKey, String largeText, String smallText) {
        lib = club.minnced.discord.rpc.DiscordRPC.INSTANCE;
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        lib.Discord_Initialize(appId, handlers, true, "");
        presence = new DiscordRichPresence();
        presence.startTimestamp = System.currentTimeMillis() / 1000;
        presence.details = firstLine;
        presence.state = secondLine;
        //Ниче не лишнее, так надо
        if (largeKey != null) {
            presence.largeImageKey = largeKey;
        }
        if (smallKey != null) {
            presence.smallImageKey = smallKey;
        }
        if (largeKey != null && largeText != null) {
            presence.largeImageText = largeText;
        }
        if (smallKey != null && smallText != null) {
            presence.smallImageText = smallText;
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
            lib.Discord_Shutdown();
        }, "RPC");
        thr.setDaemon(true);
        thr.start();
    }
}

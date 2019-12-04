package pro.gravit.launchermodules.discordrpc;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRichPresence;
import com.google.gson.Gson;
import pro.gravit.utils.helper.CommonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;

class DiscordRPC {
    static final Gson confGson = CommonHelper.newBuilder().setPrettyPrinting().serializeNulls().create();
	static club.minnced.discord.rpc.DiscordRPC lib;
    static Thread thr;

    static void onConfig(Config conf) {
        club.minnced.discord.rpc.DiscordRPC lib = club.minnced.discord.rpc.DiscordRPC.INSTANCE;
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        lib.Discord_Initialize(conf.appId, handlers, true, "");
        DiscordRichPresence presence = new DiscordRichPresence();
        presence.startTimestamp = System.currentTimeMillis() / 1000;
        presence.details = conf.firstLine;
        presence.state = conf.secondLine;
        //Ниче не лишнее, так надо
        if (conf.largeKey != null) {
            presence.largeImageKey = conf.largeKey;
        }
        if (conf.smallKey != null) {
            presence.smallImageKey = conf.smallKey;
        }
        if (conf.largeKey != null && conf.largeText != null) {
            presence.largeImageText = conf.largeText;
        }
        if (conf.smallKey != null && conf.smallText != null) {
            presence.smallImageText = conf.smallText;
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
        thr.setPriority(Integer.MIN_VALUE);
        thr.start();
    }
}

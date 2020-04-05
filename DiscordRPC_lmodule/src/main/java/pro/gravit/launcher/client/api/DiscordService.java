package pro.gravit.launcher.client.api;

import club.minnced.discord.rpc.DiscordRichPresence;
import pro.gravit.launchermodules.discordrpc.DiscordRPC;

public class DiscordService {
    private DiscordService() {
        throw new UnsupportedOperationException();
    }

    public static void apply() {
        DiscordRPC.resetPresence();
    }

    public static DiscordRichPresence getDiscordRichPresence() {
        return DiscordRPC.presence;
    }
}

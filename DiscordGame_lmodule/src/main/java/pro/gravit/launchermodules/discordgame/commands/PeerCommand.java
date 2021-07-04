package pro.gravit.launchermodules.discordgame.commands;

import de.jcm.discordgamesdk.Core;
import pro.gravit.launchermodules.discordgame.DiscordBridge;
import pro.gravit.utils.command.Command;

public class PeerCommand extends Command {
    public PeerCommand() {
        this.childCommands.put("peer", new PeerCommand());
    }

    @Override
    public String getArgsDescription() {
        return "[server/client] (port)";
    }

    @Override
    public String getUsageDescription() {
        return "test peer connection";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        Core core = DiscordBridge.getCore();
    }
}

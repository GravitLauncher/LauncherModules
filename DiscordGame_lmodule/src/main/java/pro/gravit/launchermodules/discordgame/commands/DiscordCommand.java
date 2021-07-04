package pro.gravit.launchermodules.discordgame.commands;

import pro.gravit.utils.command.Command;

public class DiscordCommand extends Command {
    @Override
    public String getArgsDescription() {
        return "[subcommand] [args...]";
    }

    @Override
    public String getUsageDescription() {
        return "discord connection manager";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}

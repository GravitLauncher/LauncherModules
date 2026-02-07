package pro.gravit.launchermodules.mirrorhelper.commands;

import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.SubCommand;

public class MirrorHelperCommand extends Command {

    private final MirrorHelperModule module;

    public MirrorHelperCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
        this.childCommands.put("setDisableDownloadAssets", new SubCommand() {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 1);
                module.config.disableDownloadAssets = Boolean.parseBoolean(args[0]);
                module.configurable.saveConfig();
            }
        });
    }

    @Override
    public String getArgsDescription() {
        return "[subcommand] [args...]";
    }

    @Override
    public String getUsageDescription() {
        return "Manage MirrorHelper";
    }

    @Override
    public void invoke(String... args) throws Exception {
        invokeSubcommands(args);
    }
}

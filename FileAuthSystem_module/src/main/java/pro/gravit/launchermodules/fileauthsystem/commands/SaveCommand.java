package pro.gravit.launchermodules.fileauthsystem.commands;

import pro.gravit.launchermodules.fileauthsystem.FileAuthSystemModule;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Paths;

public class SaveCommand extends Command {
    private final FileAuthSystemModule module;

    public SaveCommand(LaunchServer server, FileAuthSystemModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "(path)";
    }

    @Override
    public String getUsageDescription() {
        return "save database to file";
    }

    @Override
    public void invoke(String... args) throws Exception {
        if (args.length > 0) {
            module.save(Paths.get(args[0]));
        } else {
            module.save();
        }
        LogHelper.info("Database saved");
    }
}

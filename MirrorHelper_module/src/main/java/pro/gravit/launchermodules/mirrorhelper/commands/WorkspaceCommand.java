package pro.gravit.launchermodules.mirrorhelper.commands;

import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.InstallClient;
import pro.gravit.launchermodules.mirrorhelper.WorkspaceManifest;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.SubCommand;
import pro.gravit.utils.helper.IOHelper;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class WorkspaceCommand extends Command {
    public WorkspaceCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        SubCommand clearClient = new SubCommand("[vanilla/forge/neoforge] [version]", "remove client cache with specific loader and version") {
            @Override
            public void invoke(String... args) throws Exception {
                verifyArgs(args, 2);
                var loader = InstallClient.VersionType.valueOf(args[0].toUpperCase(Locale.ROOT));
                String version = WorkspaceManifest.component(parseClientVersion(args[1]).toString());
                synchronized (module) {
                    Path target = WorkspaceManifest.resolve(module.getCacheDir(), "clients/"
                            + loader.name().toLowerCase(Locale.ROOT) + "/" + version);
                    if (!Files.isDirectory(target)) {
                        throw new FileNotFoundException(target.toString());
                    }
                    IOHelper.deleteDir(target, true);
                }
            }
        };
        childCommands.put("clearclientcache", clearClient);
    }

    @Override
    public String getArgsDescription() {
        return "[command]";
    }

    @Override
    public String getUsageDescription() {
        return "workspace tools";
    }

    @Override
    public void invoke(String... strings) throws Exception {
        invokeSubcommands(strings);
    }
}

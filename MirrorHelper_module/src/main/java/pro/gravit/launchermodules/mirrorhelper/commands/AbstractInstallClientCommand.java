package pro.gravit.launchermodules.mirrorhelper.commands;

import org.jline.reader.Candidate;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.command.CommandException;
import pro.gravit.launchermodules.mirrorhelper.InstallClient;
import pro.gravit.launchermodules.mirrorhelper.MirrorWorkspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

abstract class AbstractInstallClientCommand extends Command {
    AbstractInstallClientCommand(LaunchServer server) {
        super(server);
    }

    record Target(String name, ClientProfile.Version version, InstallClient.VersionType loader) {}

    Target parseTarget(String[] args, int minimumArguments) throws CommandException {
        verifyArgs(args, minimumArguments);
        return new Target(args[0], parseClientVersion(args[1]), InstallClient.VersionType.valueOf(args[2]));
    }

    static List<String> mods(String[] args, int index, MirrorWorkspace workspace, InstallClient.VersionType loader) {
        if (args.length <= index) return workspace.mods(loader);
        return args[index].isBlank() ? List.of() : Arrays.asList(args[index].split(","));
    }

    @Override
    public List<Candidate> complete(List<String> words, int wordIndex, String word) {
        if (wordIndex == 0) {
            return List.of(new Candidate(
                    word.isEmpty() ? "" : word,
                    "<name> — Profile Name and Dir",       // display
                    null, null, null, null, false  // group, desc, suffix, key, complete
            ));
        }
        if (wordIndex == 1) {
            return List.of(new Candidate(
                    word.isEmpty() ? "" : word,
                    "<version> — Minecraft version (example: 1.22.11)",
                    null, null, null, null, false
            ));
        }
        if (wordIndex == 2) {
            return Arrays.stream(InstallClient.VersionType.values())
                    .map(Enum::name)
                    .filter(name -> name.startsWith(word))
                    .map(Candidate::new)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

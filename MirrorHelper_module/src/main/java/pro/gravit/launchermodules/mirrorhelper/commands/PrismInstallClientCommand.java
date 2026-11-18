package pro.gravit.launchermodules.mirrorhelper.commands;

import org.jline.reader.Candidate;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchermodules.mirrorhelper.InstallClient;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.MirrorWorkspace;
import pro.gravit.launchermodules.mirrorhelper.PrismInstallClient;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PrismInstallClientCommand extends Command {
    private final MirrorHelperModule module;

    public PrismInstallClientCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[name] [version] [versionType] [prismClientName] (mods)";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 4);
        String name = args[0];
        ClientProfile.Version version = parseClientVersion(args[1]);
        InstallClient.VersionType versionType = InstallClient.VersionType.valueOf(args[2]);
        synchronized (module) {
            MirrorWorkspace workspace = module.getWorkspace();
            List<String> mods = args.length > 4
                    ? (args[4].isBlank() ? List.of() : Arrays.asList(args[4].split(",")))
                    : workspace.mods(versionType);
            new PrismInstallClient(module, name, version, mods, versionType, workspace, args[3]).run();
        }
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

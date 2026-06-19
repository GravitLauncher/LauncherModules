package pro.gravit.launchermodules.mirrorhelper.commands;

import org.jline.reader.Candidate;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launchermodules.mirrorhelper.InstallClient;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchermodules.mirrorhelper.MirrorWorkspace;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InstallClientCommand extends Command {
    private final MirrorHelperModule module;

    public InstallClientCommand(LaunchServer server, MirrorHelperModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public String getArgsDescription() {
        return "[name] [version] [versionType] (mods)";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        String name = args[0];
        ClientProfile.Version version = parseClientVersion(args[1]);
        InstallClient.VersionType versionType = InstallClient.VersionType.valueOf(args[2]);
        List<String> mods = new ArrayList<>();
        MirrorWorkspace mirrorWorkspace = module.getWorkspace();
        if(mirrorWorkspace != null) {
            switch (versionType) {
                case VANILLA -> {
                }
                case FABRIC -> mods.addAll(module.getWorkspace().fabricMods());
                case FORGE -> mods.addAll(module.getWorkspace().forgeMods());
                case QUILT -> mods.addAll(module.getWorkspace().quiltMods());
            }
        }
        if (args.length > 3) {
            mods = Arrays.stream(args[3].split(",")).toList();
        }
        InstallClient run = new InstallClient(module, name, version, mods, versionType, mirrorWorkspace);
        run.run();
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

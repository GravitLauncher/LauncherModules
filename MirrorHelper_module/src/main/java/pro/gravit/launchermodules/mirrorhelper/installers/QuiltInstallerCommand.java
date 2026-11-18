package pro.gravit.launchermodules.mirrorhelper.installers;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.nio.file.Path;
import java.util.List;

public class QuiltInstallerCommand extends Command {
    public QuiltInstallerCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[minecraft version] [client directory] [installer]";
    }

    @Override
    public String getUsageDescription() {
        return "Install Quilt into a client";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        install(args[0], Path.of(args[1]), Path.of(args[2]));
    }

    public static void install(String version, Path client, Path installer) throws Exception {
        LoaderLibraries.install(client, List.of(IOHelper.resolveJavaBin(IOHelper.JVM_DIR).toString(),
                "-jar", installer.toAbsolutePath().toString(), "install", "client", version,
                "--install-dir=" + client.toAbsolutePath(), "--no-profile"));
    }
}

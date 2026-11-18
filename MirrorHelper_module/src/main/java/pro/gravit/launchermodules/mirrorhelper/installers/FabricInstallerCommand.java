package pro.gravit.launchermodules.mirrorhelper.installers;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FabricInstallerCommand extends Command {
    public FabricInstallerCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[minecraft version] [client directory] [installer] (loader version)";
    }

    @Override
    public String getUsageDescription() {
        return "Install Fabric into a client";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        install(args[0], Path.of(args[1]), Path.of(args[2]), args.length > 3 ? args[3] : null);
    }

    public static void install(String version, Path client, Path installer, String loaderVersion) throws Exception {
        List<String> command = new ArrayList<>(List.of(IOHelper.resolveJavaBin(IOHelper.JVM_DIR).toString(),
                "-jar", installer.toAbsolutePath().toString(), "client", "-dir",
                client.toAbsolutePath().toString(), "-mcversion", version));
        if (loaderVersion != null) {
            command.addAll(List.of("-loader", loaderVersion));
        }
        LoaderLibraries.install(client, command);
    }
}

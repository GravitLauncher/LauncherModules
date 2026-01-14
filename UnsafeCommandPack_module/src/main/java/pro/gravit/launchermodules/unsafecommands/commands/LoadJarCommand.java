package pro.gravit.launchermodules.unsafecommands.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.StarterAgent;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public class LoadJarCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(LoadJarCommand.class);

    public LoadJarCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[jarfile]";
    }

    @Override
    public String getUsageDescription() {
        return "Load jar file";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        Path file = Paths.get(args[0]);
        StarterAgent.inst.appendToSystemClassLoaderSearch(new JarFile(file.toFile()));
        logger.info("File {} added to system classpath", file.toAbsolutePath().toString());
    }
}
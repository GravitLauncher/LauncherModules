package pro.gravit.launcher.launchermodules.prestarter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.BinaryPipeline;
import pro.gravit.launchserver.binary.PipelineContext;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.launchserver.binary.tasks.exe.BuildExeMainTask;
import pro.gravit.utils.helper.IOHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PrestarterTask implements LauncherBuildTask, BuildExeMainTask {
    private final LaunchServer server;
    private final PrestarterModule module;

    private transient final Logger logger = LogManager.getLogger();

    public PrestarterTask(LaunchServer server, PrestarterModule module) {
        this.server = server;
        this.module = module;
    }

    @Override
    public String getName() {
        return "prestarter";
    }

    @Override
    public Path process(PipelineContext context) throws IOException {
        Path prestarterPath = Paths.get(module.config.prestarterPath);
        if(!Files.exists(prestarterPath)) {
            throw new FileNotFoundException(prestarterPath.toString());
        }
        Path outputPath = context.makeTempPath("prestarter", "exe");
        context.putProperty("checkClientSecret", server.launcherBinary.context.getProperty("checkClientSecret"));
        try(OutputStream output = IOHelper.newOutput(outputPath)) {
            try(InputStream input = IOHelper.newInput(prestarterPath)) {
                input.transferTo(output);
            }
            try(InputStream input = IOHelper.newInput(server.launcherBinary.context.getLastest())) {
                input.transferTo(output);
            }
        }
        return outputPath;
    }
}

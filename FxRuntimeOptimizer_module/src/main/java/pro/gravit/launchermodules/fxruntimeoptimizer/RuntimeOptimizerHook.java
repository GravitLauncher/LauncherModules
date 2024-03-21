package pro.gravit.launchermodules.fxruntimeoptimizer;

import javafx.css.Stylesheet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.BuildContext;
import pro.gravit.launchserver.binary.tasks.MainBuildTask;
import pro.gravit.utils.HookException;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RuntimeOptimizerHook implements MainBuildTask.IOHookSet.IOHook<BuildContext> {
    private transient final Logger logger = LogManager.getLogger(RuntimeOptimizerHook.class);
    private transient final LaunchServer server;
    private transient final FxRuntimeOptimizerModule module;

    public RuntimeOptimizerHook(LaunchServer server, FxRuntimeOptimizerModule module) {
        this.server = server;
        this.module = module;
    }

    @Override
    public void hook(BuildContext context) throws HookException, IOException {
        var currentRuntimeDir = context.getRuntimeDir();
        var tmpRuntimeDir = server.tmpDir.resolve("runtime-optimizer");
        context.setRuntimeDir(tmpRuntimeDir);
        context.setDeleteRuntimeDir(true);
        IOHelper.walk(currentRuntimeDir, new OptimizerVisitor(context, currentRuntimeDir, tmpRuntimeDir), true);
    }

    private class OptimizerVisitor extends SimpleFileVisitor<Path> {
        private final BuildContext context;
        private final Path baseDir;
        private final Path targetDir;

        private OptimizerVisitor(BuildContext context, Path baseDir, Path targetDir) {
            this.context = context;
            this.baseDir = baseDir;
            this.targetDir = targetDir;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path rel = baseDir.relativize(file);
            String fileName = rel.getFileName().toString();
            if(fileName.endsWith(".css")) {
                fileName = fileName.substring(0, fileName.length()-".css".length()).concat(".bss");
                Path target = targetDir.resolve(rel.getParent()).resolve(fileName);
                logger.trace("Convert {} to {}", file, target);
                IOHelper.createParentDirs(target);
                Stylesheet.convertToBinary(file.toFile(), target.toFile());
            } else {
                Path target = targetDir.resolve(rel);
                IOHelper.createParentDirs(target);
                IOHelper.copy(file, target);
            }
            return super.visitFile(file, attrs);
        }
    }
}

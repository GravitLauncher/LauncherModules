package pro.gravit.launchermodules.unsafecommands.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class DeDupLibrariesCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public DeDupLibrariesCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[clientDir]";
    }

    @Override
    public String getUsageDescription() {
        return "remove libraries duplication (excludes lwjgl)";
    }


    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 1);
        Path dir = server.updatesDir.resolve(args[0]).resolve("libraries");
        if (!Files.isDirectory(dir)) {
            throw new FileNotFoundException(dir.toString());
        }
        Map<String, List<Path>> map = new HashMap<>(16);
        IOHelper.walk(dir, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                if (Files.isDirectory(path) && Character.isDigit(path.getFileName().toString().charAt(0))) {
                    String basePath = path.getParent().toString();
                    List<Path> value = map.computeIfAbsent(basePath, k -> new ArrayList<>(1));
                    value.add(path);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        }, false);
        logger.info("Found {} libraries", map.size());
        for (Map.Entry<String, List<Path>> entry : map.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (value.size() > 1) {
                if (key.contains("lwjgl")) {
                    logger.trace("Path {} skipped (lwjgl found)", key);
                    continue;
                }
                //logger.info("In path {} found {} libraries", key, value.size());
                var version = value.stream()
                        .map(this::convertStringToVersion)
                        .max(Comparator.naturalOrder()).orElse(null);
                logger.info("In path {} variants [{}] selected {} version", key,
                        value.stream().map(e -> e.getFileName().toString()).collect(Collectors.joining(", ")),
                        version.originalPath.getFileName().toString());
                Path selectedPath = version.originalPath;
                for (Path path : value) {
                    if (path.equals(selectedPath)) {
                        continue;
                    }
                    logger.trace("Delete dir {}", path.toString());
                    IOHelper.deleteDir(path, true);
                }
            }
        }
    }

    private static class InternalLibraryVersion implements Comparable<InternalLibraryVersion> {
        private final long[] data;
        private final Path originalPath;

        public InternalLibraryVersion(long[] data, Path originalPath) {
            this.data = data;
            this.originalPath = originalPath;
            //logger.debug("LibraryVersion parsed: [{}]", Arrays.stream(data).mapToObj(String::valueOf).collect(Collectors.joining(", ")));
        }

        @Override
        public int compareTo(InternalLibraryVersion some) {
            int result = 0;
            for (int i = 0; i < data.length; ++i) {
                if (i > some.data.length) break;
                result = Long.compare(data[i], some.data[i]);
                if (result != 0) return result;
            }
            return result;
        }
    }

    private InternalLibraryVersion convertStringToVersion(Path path) {
        String string = path.getFileName().toString();
        string = string.replaceAll("[^.0-9]", "."); // Replace any non-digit character to .
        String[] list = string.split("\\.");
        return new InternalLibraryVersion(Arrays.stream(list)
                .filter(e -> !e.isEmpty()) // Filter ".."
                .mapToLong(Long::parseLong).toArray(), path);
    }
}

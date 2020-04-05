package pro.gravit.launchermodules.unsafecommands.patcher;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class UnsafePatcher {
    public void processDir(Path path, Path tempFile, boolean testMode) throws IOException {
        IOHelper.walk(path, new PatcherVisitor(tempFile, testMode), false);
    }

    public void processFile(Path file, Path tempFile, boolean testMode) throws IOException {
        processFile(file, tempFile);
        if (Files.exists(tempFile))
            if (testMode) {
                Files.delete(tempFile);
            } else {
                Files.delete(file);
                Files.move(tempFile, file);
            }
    }

    public void processFile(Path path, Path tempFile) throws IOException {
        LogHelper.debug("Process file %s", path.toString());
        if (path.toFile().getName().endsWith(".jar")) processJar(path, tempFile);
        else if (path.toFile().getName().endsWith(".class")) processClass(path, tempFile);
    }

    public void processClass(Path path, Path tempFile) throws IOException {
        try (OutputStream outputStream = IOHelper.newOutput(tempFile)) {
            try (InputStream inputStream = IOHelper.newInput(path)) {
                process(inputStream, outputStream);
            }
        }
    }

    public void processJar(Path path, Path tempFile) throws IOException {
        if (!path.toFile().getName().endsWith(".jar")) return;
        try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(tempFile))) {
            try (ZipInputStream input = IOHelper.newZipInput(path)) {
                ZipEntry entry = input.getNextEntry();
                while (entry != null) {
                    ZipEntry outputEntry = IOHelper.newZipEntry(entry);
                    output.putNextEntry(outputEntry);
                    processEntry(entry, input, output);
                    output.closeEntry();
                    entry = input.getNextEntry();
                }
            }
        }
    }

    public void processEntry(ZipEntry entry, ZipInputStream inputStream, ZipOutputStream outputStream) throws IOException {
        if (!entry.getName().endsWith(".class")) return;
        process(inputStream, outputStream);
    }

    public abstract void process(InputStream input, OutputStream output) throws IOException;

    public class PatcherVisitor extends SimpleFileVisitor<Path> {
        public final Path tempFile;
        public final boolean testMode;

        public PatcherVisitor(Path tempFile, boolean testMode) {
            this.tempFile = tempFile;
            this.testMode = testMode;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            processFile(file, tempFile, testMode);
            return super.visitFile(file, attrs);
        }
    }
}

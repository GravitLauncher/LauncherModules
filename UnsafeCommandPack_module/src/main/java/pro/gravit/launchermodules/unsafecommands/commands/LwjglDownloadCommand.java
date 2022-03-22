package pro.gravit.launchermodules.unsafecommands.commands;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.Downloader;
import pro.gravit.utils.helper.IOHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LwjglDownloadCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();
    public LwjglDownloadCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return null;
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        String version = args[0];
        Path clientDir = server.updatesDir.resolve(args[1]);
        Path lwjglDir = clientDir.resolve("libraries").resolve("com").resolve("org").resolve("lwjgl");
        Path natives = clientDir.resolve("natives");
        List<String> components = List.of("lwjgl", "lwjgl-stb", "lwjgl-opengl", "lwjgl-openal", "lwjgl-glfw", "lwjgl-tinyfd", "lwjgl-jemalloc");
        List<String> archs = List.of("linux", "windows", "windows-x86", "macos");
        String mirror = "https://repo1.maven.org/maven2/org/lwjgl/";
        for(String component : components) {
            Path jarPath = lwjglDir.resolve(component).resolve(version).resolve(component.concat("-").concat(version).concat("jar"));
            Path jarDirPath = jarPath.getParent();
            Files.createDirectories(jarDirPath);
            String prepareUrl = mirror
                    .concat(component)
                    .concat("/")
                    .concat(version)
                    .concat("/");
            URL jarUrl = new URL(prepareUrl
                    .concat(String.format("%s-%s.jar", component, version)));
            logger.info("Download {} to {}", jarUrl, jarPath);
            download(jarUrl, jarPath);
            List<String> processedFiles = new ArrayList<>();
            for(String arch : archs) {
                URL nativesUrl = new URL(prepareUrl
                        .concat(String.format("%s-%s-natives-%s.jar", component, version, arch)));
                logger.info("Download natives {}", nativesUrl);
                try(ZipInputStream input = new ZipInputStream(IOHelper.newInput(nativesUrl))) {
                    ZipEntry entry = input.getNextEntry();
                    while(entry != null) {
                        if(!entry.isDirectory() && !entry.getName().startsWith("META-INF")) {
                            Path path = Paths.get(entry.getName());
                            String filename = path.getFileName().toString();
                            logger.info("Process {}", filename);
                            if(processedFiles.contains(filename)) {
                                if("windows-x86".equals(arch)) {
                                    String oldName = filename;
                                    int index = filename.indexOf(".");
                                    filename = filename.substring(0, index).concat("32").concat(filename.substring(index));
                                    logger.info("Change name {} to {}", oldName, filename);
                                } else {
                                    logger.warn("Duplicate {}", filename);
                                }
                            }
                            IOHelper.transfer(input, natives.resolve(filename));
                            processedFiles.add(filename);
                        }
                        entry = input.getNextEntry();
                    }
                }
            }
            logger.info("Complete");
        }
    }

    private void download(URL url, Path target) throws IOException {
        try(InputStream input = IOHelper.newInput(url)) {
            try(OutputStream output = new FileOutputStream(target.toFile())) {
                IOHelper.transfer(input, output);
            }
        }
    }
}

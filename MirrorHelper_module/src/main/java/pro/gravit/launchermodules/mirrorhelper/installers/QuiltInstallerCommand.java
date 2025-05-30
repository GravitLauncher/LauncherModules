package pro.gravit.launchermodules.mirrorhelper.installers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class QuiltInstallerCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();

    public QuiltInstallerCommand(LaunchServer server) {
        super(server);
    }

    public static NamedURL makeURL(String mavenUrl, String mavenId) throws URISyntaxException, MalformedURLException {
        URI baseUri = new URI(mavenUrl);
        String scheme = baseUri.getScheme();
        String host = baseUri.getHost();
        int port = baseUri.getPort();
        if (port != -1)
            host = host + ":" + port;
        String path = baseUri.getPath();
        //
        String[] mavenIdSplit = mavenId.split(":");
        String artifact = "%s/%s/%s/%s-%s.jar".formatted(mavenIdSplit[0].replaceAll("\\.", "/"),
                mavenIdSplit[1], mavenIdSplit[2], mavenIdSplit[1], mavenIdSplit[2]);
        //
        URL url = new URI(scheme, host, path + artifact, "", "").toURL();
        return new NamedURL(url, artifact);
    }

    @Override
    public String getArgsDescription() {
        return "[minecraft version] [vanilla dir] [fabric installer file]";
    }

    @Override
    public String getUsageDescription() {
        return "install quilt to client";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        String version = args[0];
        Path vanillaDir = Path.of(args[1]);
        if (!Files.exists(vanillaDir)) {
            throw new FileNotFoundException(vanillaDir.toString());
        }
        Path fabricInstallerFile = Paths.get(args[2]);
        List<String> processArgs = new ArrayList<>(6);
        processArgs.add(IOHelper.resolveJavaBin(IOHelper.JVM_DIR).toString());
        processArgs.add("-jar");
        processArgs.add(fabricInstallerFile.toAbsolutePath().toString());
        processArgs.add("install");
        processArgs.add("client");
        processArgs.add(version);
        processArgs.add("--install-dir=".concat(vanillaDir.toString()));
        processArgs.add("--no-profile");
        logger.debug("Launch {}", String.join(" ", processArgs));
        Process process = new ProcessBuilder(processArgs).inheritIO().start();
        process.waitFor();
        if (!Files.exists(vanillaDir.resolve("versions"))) {
            throw new FileNotFoundException("versions not found. Quilt not installed");
        }
        Path fabricClientDir = Files.list(vanillaDir.resolve("versions")).findFirst().orElseThrow();
        Path fabricProfileFile = Files.list(fabricClientDir).filter(p -> p.getFileName().toString().endsWith(".json")).findFirst().orElseThrow();
        logger.debug("Quilt profile {}", fabricProfileFile.toString());
        MinecraftProfile fabricProfile;
        try (Reader reader = IOHelper.newReader(fabricProfileFile)) {
            fabricProfile = Launcher.gsonManager.configGson.fromJson(reader, MinecraftProfile.class);
        }
        for (MinecraftProfileLibrary library : fabricProfile.libraries) {
            NamedURL url = makeURL(library.url, library.name);
            logger.info("Download {} into {}", url.url.toString(), url.name);
            Path file = vanillaDir.resolve("libraries").resolve(url.name);
            IOHelper.createParentDirs(file);
            try (InputStream stream = IOHelper.newInput(url.url)) {
                try (OutputStream output = IOHelper.newOutput(file)) {
                    IOHelper.transfer(stream, output);
                }
            }
        }
        logger.info("Clearing...");
        IOHelper.deleteDir(vanillaDir.resolve("versions"), true);
        server.updatesManager.syncUpdatesDir(List.of(args[1]));
        logger.info("Quilt installed successful. Please use `makeprofile` command");
    }

    public static class MinecraftProfileLibrary {
        public String name;
        public String url; // maven repo
    }

    public static class MinecraftProfile {
        public List<MinecraftProfileLibrary> libraries;
    }

    public static class NamedURL {
        public final URL url;
        public final String name;

        public NamedURL(URL url, String name) {
            this.url = url;
            this.name = name;
        }
    }
}

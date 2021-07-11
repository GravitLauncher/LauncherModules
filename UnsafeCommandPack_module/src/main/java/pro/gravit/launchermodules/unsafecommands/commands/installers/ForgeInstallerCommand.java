package pro.gravit.launchermodules.unsafecommands.commands.installers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchermodules.unsafecommands.impl.NoClosingInputStream;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ForgeInstallerCommand extends Command {
    private transient final Logger logger = LogManager.getLogger();
    private boolean forgeNoConfirm = Boolean.parseBoolean(System.getProperty("modules.unsafecommandspack.forgeinstaller.noconfirm", "false"));

    public ForgeInstallerCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[vanilla dir] [forge installer file]";
    }

    @Override
    public String getUsageDescription() {
        return "install forge to client";
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 2);
        Path dir = server.updatesDir.resolve(args[0]);
        if (!Files.exists(dir)) {
            throw new FileNotFoundException(dir.toString());
        }
        Path forgeInstaller = Paths.get(args[1]);
        if (!Files.exists(forgeInstaller)) {
            throw new FileNotFoundException(forgeInstaller.toString());
        }
        logger.info("If possible please consider moving to Fabric");
        logger.info("Forge is supported by advertising when downloading and installing. Please do not use AdBlock when downloading it, this will help the project");
        logger.error("FORGE INSTALLER COMMAND IS WORK IN PROGRESS!");
        if (!forgeNoConfirm && !showApplyDialog("Continue?")) {
            return;
        }

        ForgeInstallManifest forgeInstallManifest = null;
        try (ZipInputStream input = IOHelper.newZipInput(forgeInstaller)) {
            ZipEntry entry = input.getNextEntry();
            while (entry != null) {
                String filename = entry.getName();
                if (filename.equals("install_profile.json")) {
                    logger.debug("Found install_profile.json");
                    forgeInstallManifest = readForgeInstallManifest(new NoClosingInputStream(input));
                }
                entry = input.getNextEntry();
            }
        }
        if (forgeInstallManifest == null) {
            throw new RuntimeException("Forge install manifest not found");
        }
        forgeInstallManifest.data.put("SIDE", new ServerAndClientValue("client"));
        forgeInstallManifest.data.put("MINECRAFT_JAR", new ServerAndClientValue(dir.resolve("minecraft.jar").toAbsolutePath().toString()));
        if (false) {
            logger.info("Collect libraries and processors");
            Files.createDirectory(dir.resolve("tmp"));
            for (LibraryInfo info : forgeInstallManifest.libraries) {
                if (info.downloads.artifact.url.isEmpty()) {
                    continue;
                }
                Path file = dir.resolve("tmp").resolve(info.downloads.artifact.path);
                IOHelper.createParentDirs(file);
                logger.debug("Download {}", info.downloads.artifact.url);
                try (InputStream stream = IOHelper.newInput(new URL(info.downloads.artifact.url))) {
                    try (OutputStream output = IOHelper.newOutput(file)) {
                        IOHelper.transfer(stream, output);
                    }
                }
            }
        }
        {
            logger.info("Collect install processors");
            for (InstallProcessor processor : forgeInstallManifest.processors) {
                processor.file = dir.resolve("tmp").resolve(forgeInstallManifest.findByName(processor.jar).downloads.artifact.path);
            }
            logger.info("Launch pipeline");
            for (InstallProcessor processor : forgeInstallManifest.processors) {
                List<String> processArgs = new ArrayList<>();
                processArgs.add(IOHelper.resolveJavaBin(IOHelper.JVM_DIR).toString());
                processArgs.add("-jar");
                processArgs.add(processor.file.toAbsolutePath().toString());
                for (String arg : processor.args) {
                    processArgs.add(forgeInstallManifest.dataReplace(arg));
                }
                logger.debug("Launch {}", String.join(" ", processArgs));
                Process process = new ProcessBuilder(processArgs).inheritIO().start();
                process.waitFor();
            }
        }
    }

    private ForgeInstallManifest readForgeInstallManifest(InputStream stream) throws IOException {
        try (Reader reader = new InputStreamReader(stream)) {
            return Launcher.gsonManager.configGson.fromJson(reader, ForgeInstallManifest.class);
        }
    }

    public static class LibraryArtifactInfo {
        public String path;
        public String url;
        public long size;
    }

    public static class LibraryDownloadInfo {
        public LibraryArtifactInfo artifact;
    }

    public static class LibraryInfo {
        public String name;
        public LibraryDownloadInfo downloads;
    }

    public static class ServerAndClientValue {
        public String client;
        public String server;

        public ServerAndClientValue() {
        }

        public ServerAndClientValue(String client, String server) {
            this.client = client;
            this.server = server;
        }

        public ServerAndClientValue(String client) {
            this.client = client;
        }
    }

    public static class InstallProcessor {
        public String jar; // maven id
        public transient Path file;
        public List<String> classpath;
        public List<String> args;
        public Map<String, String> outputs;
    }

    public static class ForgeInstallManifest {
        public Map<String, ServerAndClientValue> data;
        public List<InstallProcessor> processors;
        public List<LibraryInfo> libraries;

        public String dataReplace(String name) {
            ServerAndClientValue value = data.get(name);
            if (value == null) return name;
            return value.client;
        }

        public LibraryInfo findByName(String name) {
            for (LibraryInfo info : libraries) {
                if (name.equals(info.name)) {
                    return info;
                }
            }
            return null;
        }
    }
}

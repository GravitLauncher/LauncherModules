package pro.gravit.launchermodules.osslsigncode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.PipelineContext;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

public class OSSLSignTask implements LauncherBuildTask {

    private static final Logger logger =
            LoggerFactory.getLogger(OSSLSignTask.class);

    private final LaunchServer server;
    private final OSSLSignCodeConfig config;
    private LaunchServerConfig.JarSignerConf signConf;

    public OSSLSignTask(LaunchServer server, OSSLSignCodeConfig config) {
        this.server = server;
        this.config = config;
        signConf = config.customConf;
        if (signConf == null || !signConf.enabled) signConf = server.config.sign;
        if (!signConf.enabled) throw new IllegalStateException("sign.enabled must be true");
        if (!signConf.keyStoreType.equals("PKCS12"))
            throw new IllegalStateException("sign.keyStoreType must be PKCS12");
    }

    public static void signLaunch4j(OSSLSignCodeConfig config, LaunchServerConfig.JarSignerConf signConf, Path inputFile, Path resultFile) throws IOException {
        File input = new File(inputFile.toUri());
        long lastSignSize = 0;
        long inputLength = input.length();
        Files.deleteIfExists(resultFile);
        updateSignSize(inputFile, lastSignSize);
        sign(config, signConf, inputFile, resultFile);
        File output = new File(resultFile.toUri());
        long outputLength = output.length();
        long signSize = outputLength - inputLength;
        if (lastSignSize != signSize) {
            logger.debug("Saved signSize value {}, real %d", lastSignSize, signSize);
            lastSignSize = signSize;
            Files.deleteIfExists(resultFile);
            updateSignSize(inputFile, signSize);
            sign(config, signConf, inputFile, resultFile);
            if (config.checkSignSize) {
                output = new File(resultFile.toUri());
                outputLength = output.length();
                signSize = outputLength - inputLength;
                if (lastSignSize != signSize) {
                    throw new IllegalStateException("Sign check size failed. Saved: %d Real: %d".formatted(lastSignSize, signSize));
                }
            }
            if (config.checkCorrectJar) {
                try (ZipInputStream inputStream = IOHelper.newZipInput(resultFile)) {
                    inputStream.getNextEntry(); //Check
                }
            }
        }
    }

    public static void updateSignSize(Path inputFile, long signSize) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(new File(inputFile.toUri()), "rw")) {
            long fileSize = file.length();
            long offset = fileSize - 2;
            if (signSize > 0xffff) throw new IllegalArgumentException("Sign size > 65535");
            byte[] toWrite = new byte[]{(byte) (signSize & 0xff), (byte) ((signSize & 0xff00) >> 8)};
            logger.info("File size {} offset {} first byte {} last byte {}", fileSize, offset, toWrite[0], toWrite[1]);
            file.seek(offset);
            file.write(toWrite);
        }
    }

    public static void sign(OSSLSignCodeConfig config, LaunchServerConfig.JarSignerConf signConf, Path source, Path dest) throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> args = new ArrayList<>();
        args.add(config.osslsigncodePath);
        args.add("sign");
        args.add("-pkcs12");
        args.add(signConf.keyStore);
        if (config.timestampServer != null) {
            args.add("-t");
            args.add(config.timestampServer);
        }
        if (config.customArgs != null) args.addAll(config.customArgs);
        if (signConf.keyPass != null) {
            args.add("-pass");
            args.add(signConf.keyPass);
        }
        args.add("-in");
        args.add(source.toAbsolutePath().toString());
        args.add("-out");
        args.add(dest.toAbsolutePath().toString());
        builder.command(args);
        builder.inheritIO();
        Process process = builder.start();
        try {
            process.waitFor();
        } catch (InterruptedException ignored) {

        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("OSSLSignCode process return %d".formatted(process.exitValue()));
        }
    }

    @Override
    public String getName() {
        return "OSSLSign";
    }

    @Override
    public Path process(PipelineContext context) throws IOException {
        Path resultFile = context.makeTempPath("signed", "exe");
        signLaunch4j(config, signConf, context.getLastest(), resultFile);
        return resultFile;
    }

    public void sign(Path source, Path dest) throws IOException {
        OSSLSignTask.sign(config, signConf, source, dest);
    }
}
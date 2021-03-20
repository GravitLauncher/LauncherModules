package pro.gravit.launchermodules.osslsigncode;

import pro.gravit.launchserver.LaunchServer;
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
            LogHelper.debug("Saved signSize value %d, real %d", lastSignSize, signSize);
            lastSignSize = signSize;
            Files.deleteIfExists(resultFile);
            updateSignSize(inputFile, signSize);
            sign(config, signConf, inputFile, resultFile);
            if (config.checkSignSize) {
                output = new File(resultFile.toUri());
                outputLength = output.length();
                signSize = outputLength - inputLength;
                if (lastSignSize != signSize) {
                    throw new IllegalStateException(String.format("Sign check size failed. Saved: %d Real: %d", lastSignSize, signSize));
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
            LogHelper.dev("File size %d offset %d first byte %d last byte %d", fileSize, offset, toWrite[0], toWrite[1]);
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
            throw new RuntimeException(String.format("OSSLSignCode process return %d", process.exitValue()));
        }
    }

    @Override
    public String getName() {
        return "OSSLSign";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path resultFile = server.launcherEXEBinary.nextPath(getName());
        signLaunch4j(config, signConf, inputFile, resultFile);
        return resultFile;
    }

    @Override
    public boolean allowDelete() {
        return true;
    }

    public void sign(Path source, Path dest) throws IOException {
        OSSLSignTask.sign(config, signConf, source, dest);
    }
}

package pro.gravit.launchermodules.osslsigncode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.PipelineContext;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.launchserver.config.LaunchServerConfig;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

public class SigntoolTask implements LauncherBuildTask {

    private static final Logger logger =
            LoggerFactory.getLogger(SigntoolTask.class);

    private final LaunchServer server;
    private final OSSLSignCodeConfig config;
    private LaunchServerConfig.JarSignerConf signConf;

    public SigntoolTask(LaunchServer server, OSSLSignCodeConfig config) {
        this.server = server;
        this.config = config;
        signConf = config.customConf;
        if (signConf == null || !signConf.enabled) signConf = server.config.sign;
        if (!signConf.enabled) throw new IllegalStateException("sign.enabled must be true");
        if (!signConf.keyStoreType.equals("PKCS12"))
            throw new IllegalStateException("sign.keyStoreType must be PKCS12");
    }

    public static void sign(OSSLSignCodeConfig config, LaunchServerConfig.JarSignerConf signConf, Path source, Path dest) throws IOException {
        // Copy source to dest first, as signtool signs in-place
        Files.copy(source, dest);
        
        ProcessBuilder builder = new ProcessBuilder();
        List<String> args = new ArrayList<>();
        args.add(config.signtoolPath);
        args.add("sign");
        args.add("/f");
        args.add(signConf.keyStore);
        
        if (signConf.keyPass != null) {
            args.add("/p");
            args.add(signConf.keyPass);
        }
        
        if (config.timestampServer != null) {
            args.add("/tr");
            args.add(config.timestampServer);
        }
        
        if (config.signtoolCustomArgs != null) {
            args.addAll(config.signtoolCustomArgs);
        }
        
        args.add(dest.toAbsolutePath().toString());
        
        builder.command(args);
        builder.inheritIO();
        Process process = builder.start();
        try {
            process.waitFor();
        } catch (InterruptedException ignored) {
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Signtool process return %d".formatted(process.exitValue()));
        }
    }

    public static void signLaunch4j(OSSLSignCodeConfig config, LaunchServerConfig.JarSignerConf signConf, Path inputFile, Path resultFile) throws IOException {
        Files.deleteIfExists(resultFile);
        sign(config, signConf, inputFile, resultFile);
        
        if (config.checkCorrectJar) {
            try (ZipInputStream inputStream = IOHelper.newZipInput(resultFile)) {
                inputStream.getNextEntry(); //Check
            }
        }
    }

    @Override
    public String getName() {
        return "SigntoolSign";
    }

    @Override
    public Path process(PipelineContext context) throws IOException {
        Path resultFile = context.makeTempPath("signed", "exe");
        signLaunch4j(config, signConf, context.getLastest(), resultFile);
        return resultFile;
    }

    public void sign(Path source, Path dest) throws IOException {
        SigntoolTask.sign(config, signConf, source, dest);
    }
}

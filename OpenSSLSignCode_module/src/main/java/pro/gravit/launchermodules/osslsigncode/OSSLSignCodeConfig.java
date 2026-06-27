package pro.gravit.launchermodules.osslsigncode;

import pro.gravit.launchserver.config.LaunchServerConfig;

import java.util.ArrayList;
import java.util.List;

public class OSSLSignCodeConfig {
    public SigningMethod signingMethod = SigningMethod.OSSLSIGNCODE;
    public String timestampServer;
    public String osslsigncodePath;
    public String signtoolPath;
    public List<String> customArgs = new ArrayList<>();
    public List<String> signtoolCustomArgs = new ArrayList<>();
    public LaunchServerConfig.JarSignerConf customConf;

    public boolean checkSignSize = true;
    public boolean checkCorrectSign = true;
    public boolean checkCorrectJar = true;

    public enum SigningMethod {
        OSSLSIGNCODE,
        SIGNTOOL
    }

    public static OSSLSignCodeConfig getDefault() {
        OSSLSignCodeConfig config = new OSSLSignCodeConfig();
        config.timestampServer = "http://timestamp.sectigo.com";
        config.osslsigncodePath = "osslsigncode";
        config.signtoolPath = "signtool";
        config.customArgs.add("-h");
        config.customArgs.add("sha256");
        config.signtoolCustomArgs.add("/fd");
        config.signtoolCustomArgs.add("SHA256");
        config.signtoolCustomArgs.add("/td");
        config.signtoolCustomArgs.add("SHA256");
        return config;
    }
}

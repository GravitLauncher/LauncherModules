package pro.gravit.launchermodules.osslsigncode;

import pro.gravit.launchserver.config.LaunchServerConfig;

import java.util.ArrayList;
import java.util.List;

public class OSSLSignCodeConfig {
    public String timestampServer;
    public String osslsigncodePath;
    public List<String> customArgs = new ArrayList<>();
    public LaunchServerConfig.JarSignerConf customConf;

    public boolean checkSignSize = true;
    public boolean checkCorrectSign = true;
    public boolean checkCorrectJar = true;

    public static OSSLSignCodeConfig getDefault() {
        OSSLSignCodeConfig config = new OSSLSignCodeConfig();
        config.timestampServer = "http://timestamp.sectigo.com";
        config.osslsigncodePath = "osslsigncode";
        config.customArgs.add("-h");
        config.customArgs.add("sha256");
        return config;
    }
}

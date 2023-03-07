package pro.gravit.launchermodules.sentryproguardupload;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public String sentryCliPath = "sentry-cli";
    public String authToken = "YOUR_TOKEN";
    public String org = "YOUR_ORGANIZATION";
    public String project = "YOUR_PROJECT";
    public String mappingPath = "proguard/mapping.pro";
    public List<String> customArgs = new ArrayList<>();
}

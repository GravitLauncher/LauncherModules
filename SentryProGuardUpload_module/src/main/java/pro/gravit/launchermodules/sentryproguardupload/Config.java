package pro.gravit.launchermodules.sentryproguardupload;

import java.util.ArrayList;
import java.util.List;

public class Config {
    public String sentryCliPath = "sentry-cli";
    public String authToken = "YOUR_TOKEN";
    public String org = "YOUR_ORGANIZATION";
    public String project = "YOUR_PROJECT";
    public String url = "https://sentry.io/";
    public String mappingPath = "proguard/mappings.pro";
    public List<String> customArgs = new ArrayList<>();
    public List<String> customArgsBefore = new ArrayList<>();
}

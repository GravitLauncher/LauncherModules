package pro.gravit.launchermodules.discordgame;

import java.util.HashMap;
import java.util.Map;

public class ScopeConfig {
    private final String details;
    private final String state;
    private final String largeImageKey;
    private final String smallImageKey;
    private final String largeImageText;
    private final String smallImageText;

    public ScopeConfig(String details, String state, String largeImageKey, String smallImageKey, String largeImageText, String smallImageText) {
        this.details = details;
        this.state = state;
        this.largeImageKey = largeImageKey;
        this.smallImageKey = smallImageKey;
        this.largeImageText = largeImageText;
        this.smallImageText = smallImageText;
    }

    public ScopeConfig(Map<String, String> scopeConfig) {

        this.details = scopeConfig.get("details");
        this.state = scopeConfig.get("state");
        this.largeImageKey = scopeConfig.get("largeImageKey");
        this.smallImageKey = scopeConfig.get("smallImageKey");
        this.largeImageText = scopeConfig.get("largeImageText");
        this.smallImageText = scopeConfig.get("smallImageText");
    }

    public String getDetails() {
        return details;
    }

    public String getState() {
        return state;
    }

    public String getLargeImageKey() {
        return largeImageKey;
    }

    public String getSmallImageKey() {
        return smallImageKey;
    }

    public String getLargeImageText() {
        return largeImageText;
    }

    public String getSmallImageText() {
        return smallImageText;
    }

    public Map<String, String> toMap() {
        Map<String, String> scopeConfig = new HashMap<>();
        scopeConfig.put("details", this.details);
        scopeConfig.put("state", this.state);
        scopeConfig.put("largeImageKey", this.largeImageKey);
        scopeConfig.put("smallImageKey", this.smallImageKey);
        scopeConfig.put("largeImageText", this.largeImageText);
        scopeConfig.put("smallImageText", this.smallImageText);
        return scopeConfig;
    }

    @Override
    public String toString() {
        return "ScopeConfig{" +
                "details='" + details + '\'' +
                ", state='" + state + '\'' +
                ", largeImageKey='" + largeImageKey + '\'' +
                ", smallImageKey='" + smallImageKey + '\'' +
                ", largeImageText='" + largeImageText + '\'' +
                ", smallImageText='" + smallImageText + '\'' +
                '}';
    }
}

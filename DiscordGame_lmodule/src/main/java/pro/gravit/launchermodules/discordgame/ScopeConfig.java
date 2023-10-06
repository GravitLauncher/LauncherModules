package pro.gravit.launchermodules.discordgame;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScopeConfig {
    private final String details;
    private final String state;
    private final String largeImageKey;
    private final String smallImageKey;
    private final String largeImageText;
    private final String smallImageText;
    private final boolean firstButtonEnable;
    private final String firstButtonName;
    private final String firstButtonUrl;
    private final boolean secondButtonEnable;
    private final String secondButtonName;
    private final String secondButtonUrl;

    public ScopeConfig(String details, String state, String largeImageKey, String smallImageKey, String largeImageText, String smallImageText, boolean firstButtonEnable, String firstButtonName, String firstButtonUrl, boolean secondButtonEnable, String secondButtonName, String secondButtonUrl) {
        this.details = details;
        this.state = state;
        this.largeImageKey = largeImageKey;
        this.smallImageKey = smallImageKey;
        this.largeImageText = largeImageText;
        this.smallImageText = smallImageText;
        this.firstButtonEnable = firstButtonEnable;
        this.firstButtonName = firstButtonName;
        this.firstButtonUrl = firstButtonUrl;
        this.secondButtonEnable = secondButtonEnable;
        this.secondButtonName = secondButtonName;
        this.secondButtonUrl = secondButtonUrl;
    }

    public ScopeConfig(Map<String, String> scopeConfig) {

        this.details = scopeConfig.get("details");
        this.state = scopeConfig.get("state");
        this.largeImageKey = scopeConfig.get("largeImageKey");
        this.smallImageKey = scopeConfig.get("smallImageKey");
        this.largeImageText = scopeConfig.get("largeImageText");
        this.smallImageText = scopeConfig.get("smallImageText");
        this.firstButtonEnable = Boolean.parseBoolean(scopeConfig.get("firstButtonEnable"));
        this.firstButtonName = scopeConfig.get("firstButtonName");
        this.firstButtonUrl = scopeConfig.get("firstButtonUrl");
        this.secondButtonEnable = Boolean.parseBoolean(scopeConfig.get("secondButtonEnable"));
        this.secondButtonName = scopeConfig.get("secondButtonName");
        this.secondButtonUrl = scopeConfig.get("secondButtonUrl");
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

    public boolean isFirstButtonEnable() {
        return firstButtonEnable;
    }

    public String getFirstButtonName() {
        return firstButtonName;
    }

    public String getFirstButtonUrl() {
        return firstButtonUrl;
    }

    public boolean isSecondButtonEnable() {
        return secondButtonEnable;
    }

    public String getSecondButtonName() {
        return secondButtonName;
    }

    public String getSecondButtonUrl() {
        return secondButtonUrl;
    }

    public Map<String, String> toMap() {
        Map<String, String> scopeConfig = new LinkedHashMap<>();
        scopeConfig.put("details", this.details);
        scopeConfig.put("state", this.state);
        scopeConfig.put("largeImageKey", this.largeImageKey);
        scopeConfig.put("smallImageKey", this.smallImageKey);
        scopeConfig.put("largeImageText", this.largeImageText);
        scopeConfig.put("smallImageText", this.smallImageText);
        scopeConfig.put("firstButtonEnable", Boolean.toString(firstButtonEnable));
        scopeConfig.put("firstButtonName", firstButtonName);
        scopeConfig.put("firstButtonUrl", firstButtonUrl);
        scopeConfig.put("secondButtonEnable", Boolean.toString(secondButtonEnable));
        scopeConfig.put("secondButtonName", secondButtonName);
        scopeConfig.put("secondButtonUrl", secondButtonUrl);
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
                ", firstButtonEnable=" + firstButtonEnable +
                ", firstButtonName='" + firstButtonName + '\'' +
                ", firstButtonUrl='" + firstButtonUrl + '\'' +
                ", secondButtonEnable=" + secondButtonEnable +
                ", secondButtonName='" + secondButtonName + '\'' +
                ", secondButtonUrl='" + secondButtonUrl + '\'' +
                '}';
    }
}

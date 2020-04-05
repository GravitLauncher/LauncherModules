package pro.gravit.launchermodules.discordrpc;

import pro.gravit.launcher.LauncherInject;

import java.util.HashMap;
import java.util.Map;

public class Config {
    @LauncherInject(value = "modules.discordrpc.appid")
    public String appId;
    @LauncherInject(value = "modules.discordrpc.firstline")
    public String firstLine;
    @LauncherInject(value = "modules.discordrpc.secondline")
    public String secondLine;
    @LauncherInject(value = "modules.discordrpc.largekey")
    public String largeKey;
    @LauncherInject(value = "modules.discordrpc.smallkey")
    public String smallKey;
    @LauncherInject(value = "modules.discordrpc.largetext")
    public String largeText;
    @LauncherInject(value = "modules.discordrpc.smalltext")
    public String smallText;
    @LauncherInject(value = "modules.discordrpc.usealt")
    public boolean useAlt;
    @LauncherInject(value = "modules.discordrpc.altappid")
    public String altAppId;
    @LauncherInject(value = "modules.discordrpc.altfirstline")
    public String altFirstLine;
    @LauncherInject(value = "modules.discordrpc.altsecondline")
    public String altSecondLine;
    @LauncherInject(value = "modules.discordrpc.altauthorizedfirstline")
    public String altAuthorizedFirstLine;
    @LauncherInject(value = "modules.discordrpc.altauthorizedsecondline")
    public String altAuthorizedSecondLine;
    @LauncherInject(value = "modules.discordrpc.altlargekey")
    public String altLargeKey;
    @LauncherInject(value = "modules.discordrpc.altsmallkey")
    public String altSmallKey;
    @LauncherInject(value = "modules.discordrpc.altlargetext")
    public String altLargeText;
    @LauncherInject(value = "modules.discordrpc.altsmalltext")
    public String altSmallText;
    @LauncherInject(value = "modules.discordrpc.profilenamekeymappings")
    public Map<String, String> profileNameKeyMappings;

    public static Object getDefault() {
        Config config = new Config();
        config.appId = "mySuperApp";
        config.firstLine = "Играет на %profileName%";
        config.secondLine = "Ник: %username%";
        config.largeKey = "large";
        config.smallKey = "small";
        config.largeText = "Everything";
        config.smallText = "Everything";
        config.useAlt = true;
        config.altAppId = "mySuperApp";
        config.altFirstLine = "В лаунчере";
        config.altSecondLine = "Авторизируется";
        config.altAuthorizedFirstLine = "В лаунчере";
        config.altAuthorizedSecondLine = "Ник: %username%";
        config.altLargeKey = "large";
        config.altSmallKey = "small";
        config.altLargeText = "Everything";
        config.altSmallText = "Everything";
        config.profileNameKeyMappings = new HashMap<>();
        return config;
    }
}

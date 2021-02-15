package pro.gravit.launchermodules.discordgame;

import pro.gravit.launcher.LauncherInject;

import java.util.HashMap;
import java.util.Map;

public class Config {
    @LauncherInject(value = "modules.discordgame.enable")
    public boolean enable;
    @LauncherInject(value = "modules.discordgame.appid")
    public long appId;
    @LauncherInject(value = "modules.discordgame.launcherdetails")
    public String launcherDetails;
    @LauncherInject(value = "modules.discordgame.launcherstate")
    public String launcherState;
    @LauncherInject(value = "modules.discordgame.largekey")
    public String largeKey;
    @LauncherInject(value = "modules.discordgame.smallkey")
    public String smallKey;
    @LauncherInject(value = "modules.discordgame.largetext")
    public String largeText;
    @LauncherInject(value = "modules.discordgame.smalltext")
    public String smallText;
    @LauncherInject(value = "modules.discordgame.clientdetails")
    public String clientDetails;
    @LauncherInject(value = "modules.discordgame.clientstate")
    public String clientState;
    @LauncherInject(value = "modules.discordgame.authorizeddetails")
    public String authorizedDetails;
    @LauncherInject(value = "modules.discordgame.authorizedstate")
    public String authorizedState;
    @LauncherInject(value = "modules.discordgame.clientlargekey")
    public String clientLargeKey;
    @LauncherInject(value = "modules.discordgame.clientsmallkey")
    public String clientSmallKey;
    @LauncherInject(value = "modules.discordgame.clientlargetext")
    public String clientLargeText;
    @LauncherInject(value = "modules.discordgame.clientsmalltext")
    public String clientSmallText;
    @LauncherInject(value = "modules.discordgame.profilenamekeymappings")
    public Map<String, String> profileNameKeyMappings;

    public static Object getDefault() {
        Config config = new Config();
        config.enable = true;
        config.appId = 810913859371532298L;
        config.launcherDetails = "Лучший проект Minecraft";
        config.clientDetails = "Лучший проект Minecraft";
        config.authorizedDetails = "Лучший проект Minecraft";
        config.launcherState = "В лаунчере";
        config.authorizedState = "Выбирает сервер";
        config.clientState = "Играет на %profileName%";
        config.largeKey = "large";
        config.smallKey = "small";
        config.largeText = "Everything";
        config.smallText = "Everything";
        config.clientLargeKey = "large";
        config.clientSmallKey = "small";
        config.clientLargeText = "Everything";
        config.clientSmallText = "Everything";
        config.profileNameKeyMappings = new HashMap<>();
        return config;
    }
}

package pro.gravit.launchermodules.discordgame;

import pro.gravit.launcher.LauncherInject;

import java.util.*;

public class Config {
    @LauncherInject(value = "modules.discordgame.enable")
    public boolean enable;
    @LauncherInject(value = "modules.discordgame.appid")
    public long appId;
    @LauncherInject(value = "modules.discordgame.scopes")
    public Map<String, Map<String, String>> scopes;

    public static Object getDefault() {
        Config config = new Config();
        config.enable = true;
        config.appId = 810913859371532298L;

        config.scopes = new HashMap<>();

        config.scopes.put("login",
                new ScopeConfig("Лучший проект Minecraft", "Авторизуется",
                        "large", "small", "Everything", "Everything").toMap());
        config.scopes.put("authorized",
                new ScopeConfig("Лучший проект Minecraft", "Выбирает сервер",
                        "large", "small", "Everything", "Everything").toMap());
        config.scopes.put("client",
                new ScopeConfig("Лучший проект Minecraft", "Играет на %profileName%",
                        "large", "small", "Everything", "Everything").toMap());

        return config;
    }
}

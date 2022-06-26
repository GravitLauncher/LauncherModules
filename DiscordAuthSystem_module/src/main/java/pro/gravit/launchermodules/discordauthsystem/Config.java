package pro.gravit.launchermodules.discordauthsystem;

import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.util.*;

public class Config {
    public String clientId = "clientId";
    public String clientSecret = "clientSecret";
    public String redirectUrl = "redirectUrl";
    public String discordAuthorizeUrl = "https://discord.com/oauth2/authorize";
    public String discordApiEndpointVersion = "https://discord.com/api/v10";
    public String discordApiEndpoint = "https://discord.com/api";

    public List<DiscordGuild> guildIdsJoined = new ArrayList<>();

    public String guildIdGetNick = "";

    public int usernameLimit = 32;

    public static class DiscordGuild {
        public String id;
        public String name;
        public String url;

        public DiscordGuild(String id, String name, String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }
}


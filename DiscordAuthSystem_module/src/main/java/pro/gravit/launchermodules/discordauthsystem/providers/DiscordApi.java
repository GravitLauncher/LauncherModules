package pro.gravit.launchermodules.discordauthsystem.providers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchermodules.discordauthsystem.Config;

import java.io.IOException;

public class DiscordApi {
    private static final String GRANT_TYPE_AUTHORIZATION = "authorization_code";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static Config config;
    private static Logger logger;

    public static void initialize(Config config) {
        DiscordApi.config = config;
        DiscordApi.logger = LogManager.getLogger();
    }

    public static DiscordAccessTokenResponse sendRefreshToken(String refreshToken) throws IOException {
        Connection request = Jsoup.connect(config.discordApiEndpointVersion + "/oauth2/token")
                .data("client_id", config.clientId)
                .data("client_secret", config.clientSecret)
                .data("grant_type", GRANT_TYPE_REFRESH_TOKEN)
                .data("refresh_token", refreshToken)
                .ignoreContentType(true);


        return Launcher.gsonManager.gson.fromJson(
                request.post().body().text(),
                DiscordAccessTokenResponse.class
        );
    }

    public static DiscordAccessTokenResponse getAccessTokenByCode(String code) throws IOException {
        Connection request = Jsoup.connect(config.discordApiEndpointVersion + "/oauth2/token")
                .data("client_id", config.clientId)
                .data("client_secret", config.clientSecret)
                .data("grant_type", GRANT_TYPE_AUTHORIZATION)
                .data("code", code)
                .data("redirect_uri", config.redirectUrl)
                .data("scope", "")
                .ignoreContentType(true);


        return Launcher.gsonManager.gson.fromJson(
                request.post().body().text(),
                DiscordAccessTokenResponse.class
        );
    }

    public static OauthMeResponse getDiscordUserByAccessToken(String accessToken) throws IOException {

        org.jsoup.Connection request = Jsoup.connect(config.discordApiEndpoint + "/oauth2/@me")
                .header("Authorization", "Bearer " + accessToken)
                .ignoreContentType(true);

        return Launcher.gsonManager.gson.fromJson(
                request.get().body().text(),
                OauthMeResponse.class
        );
    }

    public static class DiscordUserResponse {
        public String id;
        public String username;
        public String discriminator;
        public String avatar;
        public String verified;
        public String email;
        public Integer flags;
        public String banner;
        public Integer accent_color;
        public Integer premium_type;
        public Integer public_flags;

        public DiscordUserResponse(String id, String username, String discriminator, String avatar, String verified, String email, Integer flags, String banner, Integer accent_color, Integer premium_type, Integer public_flags) {
            this.id = id;
            this.username = username;
            this.discriminator = discriminator;
            this.avatar = avatar;
            this.verified = verified;
            this.email = email;
            this.flags = flags;
            this.banner = banner;
            this.accent_color = accent_color;
            this.premium_type = premium_type;
            this.public_flags = public_flags;
        }
    }

    public static class OauthMeResponse {
        public String[] scopes;
        public String expires;
        public DiscordUserResponse user;

        public OauthMeResponse(String[] scopes, String expires, DiscordUserResponse user) {
            this.scopes = scopes;
            this.expires = expires;
            this.user = user;
        }
    }

    public static class DiscordAccessTokenResponse {
        public String access_token;
        public String token_type;
        public long expires_in;
        public String refresh_token;
        public String scope;

        public DiscordAccessTokenResponse(String access_token, String token_type, long expires_in, String refresh_token, String scope) {
            this.access_token = access_token;
            this.token_type = token_type;
            this.expires_in = expires_in;
            this.refresh_token = refresh_token;
            this.scope = scope;
        }
    }
}

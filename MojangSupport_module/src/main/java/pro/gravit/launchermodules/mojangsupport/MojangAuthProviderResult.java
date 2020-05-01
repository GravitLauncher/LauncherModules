package pro.gravit.launchermodules.mojangsupport;

import pro.gravit.launchserver.auth.provider.AuthProviderResult;

import java.util.UUID;

public final class MojangAuthProviderResult extends AuthProviderResult {
    public final UUID uuid;
    public final String launcherToken;

    public MojangAuthProviderResult(String username, String accessToken, UUID uuid, String launcherToken) {
        super(username, accessToken);
        this.uuid = uuid;
        this.launcherToken = launcherToken;
    }
}

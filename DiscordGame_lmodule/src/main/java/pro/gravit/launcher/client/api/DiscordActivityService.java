package pro.gravit.launcher.client.api;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.activity.Activity;
import pro.gravit.launcher.client.ClientLauncherProcess;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launchermodules.discordgame.ClientModule;
import pro.gravit.launchermodules.discordgame.Config;
import pro.gravit.launchermodules.discordgame.DiscordBridge;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordActivityService {
    private final Map<String, String> params = new ConcurrentHashMap<>();
    private String details;
    private String state;
    private String largeKey;
    private String smallKey;
    private String largeText;
    private String smallText;
    private String partyId;
    private int partySize;
    private int partyMaxSize;

    public DiscordActivityService() {
        setParam("launcherVersion", Version.getVersion().getVersionString());
        setParam("javaVersion", Integer.toString(JVMHelper.JVM_VERSION));
        setParam("javaBits", Integer.toString(JVMHelper.JVM_BITS));
        setParam("os", JVMHelper.OS_TYPE.name);
    }

    public void applyToActivity(Activity activity) {
        activity.setDetails(details);
        activity.setState(state);
        if (largeKey != null) {
            activity.assets().setLargeImage(largeKey);
        }
        if (smallKey != null) {
            activity.assets().setSmallImage(smallKey);
        }
        if (smallText != null) {
            activity.assets().setSmallText(smallText);
        }
        if (largeText != null) {
            activity.assets().setSmallText(largeText);
        }
        if (partyId != null) {
            activity.party().setID(partyId);
        }
        if (partyMaxSize != 0) {
            activity.party().size().setCurrentSize(partySize);
            activity.party().size().setMaxSize(partyMaxSize);
        }
    }

    public void updateActivity() {
        Core core = DiscordBridge.getCore();
        if (core == null) return;
        applyToActivity(DiscordBridge.getActivity());
        core.activityManager().updateActivity(DiscordBridge.getActivity());
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = replaceParams(details);
        updateActivity();
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = replaceParams(state);
        updateActivity();
    }

    public String getLargeKey() {
        return largeKey;
    }

    public void setLargeKey(String largeKey) {
        this.largeKey = replaceParams(largeKey);
        updateActivity();
    }

    public String getSmallKey() {
        return smallKey;
    }

    public void setSmallKey(String smallKey) {
        this.smallKey = replaceParams(smallKey);
        updateActivity();
    }

    public String getLargeText() {
        return largeText;
    }

    public void setLargeText(String largeText) {
        this.largeText = replaceParams(largeText);
        updateActivity();
    }

    public String getSmallText() {
        return smallText;
    }

    public void setSmallText(String smallText) {
        this.smallText = replaceParams(smallText);
        updateActivity();
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = replaceParams(partyId);
        updateActivity();
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
        updateActivity();
    }

    public int getPartyMaxSize() {
        return partyMaxSize;
    }

    public void setPartyMaxSize(int partyMaxSize) {
        this.partyMaxSize = partyMaxSize;
        updateActivity();
    }

    public void setParam(String key, String value) {
        String old = params.put(key, value);
        if (old != null) {
            updateActivity();
        }
    }

    public String getParam(String key) {
        return params.get(key);
    }

    public Map<String, String> getUnmodifiableParamsMap() {
        return Collections.unmodifiableMap(params);
    }

    public String replaceParams(String str) {
        if (str == null) return null;
        String result = str;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() == null) {
                LogHelper.warning("DiscordGame: Param %s null", e.getKey());
                continue;
            }
            result = result.replaceAll("%" + e.getKey() + "%", e.getValue());
        }
        return result;
    }

    public void onLauncherStart() {
        Config config = ClientModule.config;
        setDetails(config.launcherDetails);
        setState(config.launcherState);
        setLargeKey(config.largeKey);
        setLargeText(config.largeText);
        setSmallKey(config.smallKey);
        setSmallText(config.smallText);
    }

    public void onClientStart(ClientLauncherProcess.ClientParams params) {
        setParam("profileVersion", params.profile.getVersion().name);
        setParam("profileName", params.profile.getTitle());
        setParam("profileUUID", params.profile.getUUID().toString());
        setParam("profileHash", params.profile.getUUID().toString().replaceAll("-", ""));
        onPlayerProfile(params.playerProfile);
        Config config = ClientModule.config;
        setDetails(config.clientDetails);
        setState(config.clientState);
        setLargeKey(config.clientLargeKey);
        setLargeText(config.clientLargeText);
        setSmallKey(config.clientSmallKey);
        setSmallText(config.clientSmallText);
    }

    public void onPlayerProfile(PlayerProfile playerProfile) {
        setParam("username", playerProfile.username);
        setParam("uuid", playerProfile.uuid.toString());
        if (playerProfile.skin != null) {
            setParam("skinurl", playerProfile.skin.url);
        }
        if (playerProfile.cloak != null) {
            setParam("cloakurl", playerProfile.cloak.url);
        }
    }

    public void onLauncherAuth(PlayerProfile playerProfile) {
        onPlayerProfile(playerProfile);
        Config config = ClientModule.config;
        setDetails(config.authorizedDetails);
        setState(config.authorizedState);
    }

    public void resetStartTime() {
        DiscordBridge.getActivity().timestamps().setStart(Instant.now());
    }
}

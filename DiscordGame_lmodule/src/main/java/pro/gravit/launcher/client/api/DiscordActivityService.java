package pro.gravit.launcher.client.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityButton;
import de.jcm.discordgamesdk.activity.ActivityButtonsMode;
import pro.gravit.launcher.client.ClientParams;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launchermodules.discordgame.ClientModule;
import pro.gravit.launchermodules.discordgame.DiscordBridge;
import pro.gravit.launchermodules.discordgame.ScopeConfig;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordActivityService {

    private static final Logger logger =
            LoggerFactory.getLogger(DiscordActivityService.class);

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
    private final List<ActivityButton> buttons = new ArrayList<>(2);

    public DiscordActivityService() {
        setParam("launcherVersion", Version.getVersion().getVersionString());
        setParam("javaVersion", Integer.toString(JVMHelper.JVM_VERSION));
        setParam("javaBits", Integer.toString(JVMHelper.JVM_BITS));
        setParam("os", JVMHelper.OS_TYPE.name);
    }

    public void applyToActivity(Activity activity) {
        if (details != null) {
            activity.setDetails(details);
        }
        if (state != null) {
            activity.setState(state);
        }
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
            activity.assets().setLargeText(largeText);
        }
        if (partyId != null) {
            activity.party().setID(partyId);
        }
        if (partyMaxSize != 0) {
            activity.party().size().setCurrentSize(partySize);
            activity.party().size().setMaxSize(partyMaxSize);
        }
        if(!buttons.isEmpty()) {
            activity.setActivityButtonsMode(ActivityButtonsMode.BUTTONS);
            List<ActivityButton> oldButtons = new ArrayList<>(activity.getButtons());
            for(var e : oldButtons) {
                activity.removeButton(e);
            }
            for(var e : buttons) {
                activity.addButton(e);
            }
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
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = replaceParams(state);
    }

    public String getLargeKey() {
        return largeKey;
    }

    public void setLargeKey(String largeKey) {
        this.largeKey = replaceParams(largeKey);
    }

    public String getSmallKey() {
        return smallKey;
    }

    public void setSmallKey(String smallKey) {
        this.smallKey = replaceParams(smallKey);
    }

    public String getLargeText() {
        return largeText;
    }

    public void setLargeText(String largeText) {
        this.largeText = replaceParams(largeText);
    }

    public String getSmallText() {
        return smallText;
    }

    public void setSmallText(String smallText) {
        this.smallText = replaceParams(smallText);
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

    public void addButton(ActivityButton button) {
        buttons.add(button);
    }

    public void clearButtons() {
        buttons.clear();
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
                logger.warn("DiscordGame: Param {} null", e.getKey());
                continue;
            }
            result = result.replaceAll("%" + e.getKey() + "%", e.getValue());
        }
        return result;
    }

    public void updateLoginStage() {
        setParam("username", "");
        setParam("uuid", "");
        setScope(ClientModule.loginScopeConfig);
    }

    public void updateAuthorizedStage(PlayerProfile playerProfile) {
        onPlayerProfile(playerProfile);
        setScope(ClientModule.authorizedScopeConfig);
    }

    public void updateClientStage(ClientParams params) {
        setParam("profileVersion", params.profile.getVersion().toString());
        setParam("profileName", params.profile.getTitle());
        setParam("profileUUID", params.profile.getUUID().toString());
        setParam("profileHash", params.profile.getUUID().toString().replaceAll("-", ""));
        onPlayerProfile(params.playerProfile);
        setScope(ClientModule.clientScopeConfig);
    }

    public void onPlayerProfile(PlayerProfile playerProfile) {
        setParam("username", playerProfile.username);
        setParam("uuid", playerProfile.uuid.toString());
        if (playerProfile.assets.get("AVATAR") != null) {
            setParam("avatarUrl", playerProfile.assets.get("AVATAR").url);
        }
    }

    private void setScope(ScopeConfig scopeConfig) {
        logger.info("{}", scopeConfig.toString());
        setDetails(scopeConfig.getDetails());
        setState(scopeConfig.getState());
        setLargeKey(scopeConfig.getLargeImageKey());
        setLargeText(scopeConfig.getLargeImageText());
        setSmallKey(scopeConfig.getSmallImageKey());
        setSmallText(scopeConfig.getSmallImageText());
        clearButtons();
        if(scopeConfig.isFirstButtonEnable()) {
            addButton(new ActivityButton(scopeConfig.getFirstButtonName(), scopeConfig.getFirstButtonUrl()));
        }
        if(scopeConfig.isSecondButtonEnable()) {
            addButton(new ActivityButton(scopeConfig.getSecondButtonName(), scopeConfig.getSecondButtonUrl()));
        }
        updateActivity();
    }

    public void resetStartTime() {
        DiscordBridge.getActivity().timestamps().setStart(Instant.now());
    }
}
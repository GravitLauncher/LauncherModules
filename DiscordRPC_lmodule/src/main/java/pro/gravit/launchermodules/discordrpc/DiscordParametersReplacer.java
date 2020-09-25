package pro.gravit.launchermodules.discordrpc;

import java.util.UUID;

public class DiscordParametersReplacer {
    public String username;
    public String profileName;
    public String profileNameMapped;
    public String userUUID;
    public String minecraftVersion;

    public static String mappedProfileName(String profileName, UUID profileUUID) {
        if (ClientModule.config == null || ClientModule.config.profileNameKeyMappings == null) return profileName;
        String result = ClientModule.config.profileNameKeyMappings.get(profileName);
        if (result == null && profileUUID != null)
            result = ClientModule.config.profileNameKeyMappings.get(profileUUID.toString());
        if (result == null)
            result = profileName;
        return result;
    }

    public String replace(String str) {
        String result = str;
        if (username != null) result = result.replaceAll("%username%", username);
        if (profileName != null) result = result.replaceAll("%profileName%", profileName);
        if (userUUID != null) result = result.replaceAll("%userUUID%", userUUID);
        if (minecraftVersion != null) result = result.replaceAll("%minecraftVersion%", minecraftVersion);
        if (profileNameMapped != null) result = result.replaceAll("%profileNameMapped%", profileNameMapped);
        return result;
    }
}

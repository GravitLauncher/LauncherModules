package pro.gravit.launchermodules.discordrpc;

public class DiscordParametersReplacer {
    public String username;
    public String profileName;
    public String userUUID;
    public String minecraftVersion;
    public String replace(String str)
    {
        String result = str;
        if(username != null) result = result.replace("%username%", username);
        if(profileName != null) result = result.replace("%profileName%", profileName);
        if(userUUID != null) result = result.replace("%userUUID%", userUUID);
        if(minecraftVersion != null) result = result.replace("%minecraftVersion%", minecraftVersion);
        return result;
    }
}

package pro.gravit.launchermodules.mirrorhelper.config;

import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launchermodules.mirrorhelper.InstallClient;

import java.util.ArrayList;
import java.util.List;

public class ClientBuildInfo {
    public String name;
    public String version;
    public InstallClient.VersionType versionType;
    public List<Long> mods = new ArrayList<>();
    public List<String> includeDir = new ArrayList<>();
    public List<String> removeFiles = new ArrayList<>();

    public static ClientBuildInfo merge(ClientBuildInfo base, List<ClientBuildInfo> adds) {
        for(var b : adds) {
            if(base.name == null) {
                base.name = b.name;
            }
            if(base.version == null) {
                base.version = b.version;
            }
            if(base.versionType == null) {
                base.versionType = b.versionType;
            }
            base.mods.addAll(b.mods);
            base.includeDir.addAll(b.includeDir);
            base.removeFiles.addAll(b.removeFiles);
        }
        return base;
    }
}

package pro.gravit.launchermodules.mirrorhelper;

import pro.gravit.launcher.base.profiles.ClientProfile;

import java.util.List;
import java.util.Map;

public record MirrorWorkspace(List<String> fabricMods, List<String> quiltMods, List<String> forgeMods, String lwjgl3version, String fabricLoaderVersion, Map<String, MultiMod> multiMods, List<Library> libraries, Map<String, BuildScript> build) {
    public record Library(String path, String url, String data, Map<String, String> unpack, List<String> prefixFilter) {
    }
    public record MultiMod(ClientProfile.Version minVersion, ClientProfile.Version maxVersion, InstallClient.VersionType type, String url) {
        boolean check(InstallClient.VersionType type, ClientProfile.Version version) {
            if(this.type != null && this.type != type) {
                return false;
            }
            if(this.minVersion != null && version.compareTo(this.minVersion) < 0) {
                return false;
            }
            if(this.maxVersion != null && version.compareTo(this.maxVersion) > 0) {
                return false;
            }
            return true;
        }
    }

    public record BuildScript(List<BuildCommand> script, String result, String path, InstallClient.VersionType type, ClientProfile.Version minVersion, ClientProfile.Version maxVersion) {
        boolean check(InstallClient.VersionType type, ClientProfile.Version version) {
            if(this.type != null && this.type != type) {
                return false;
            }
            if(this.minVersion != null && version.compareTo(this.minVersion) < 0) {
                return false;
            }
            if(this.maxVersion != null && version.compareTo(this.maxVersion) > 0) {
                return false;
            }
            return true;
        }
    }

    public record BuildCommand(String workdir, List<String> cmd, boolean ignoreErrorCode, Map<String, String> env) {

    }
}

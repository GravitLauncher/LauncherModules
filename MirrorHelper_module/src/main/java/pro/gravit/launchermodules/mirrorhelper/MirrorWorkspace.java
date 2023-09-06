package pro.gravit.launchermodules.mirrorhelper;

import java.util.List;
import java.util.Map;

public record MirrorWorkspace(List<String> fabricMods, List<String> quiltMods, List<String> forgeMods, String lwjgl3version, String fabricLoaderVersion, Map<String, MultiMod> multiMods, List<Library> libraries, Map<String, BuildScript> build) {
    public record Library(String path, String url, String data, Map<String, String> unpack, List<String> prefixFilter) {
    }
    public record MultiMod(String minVersion, String maxVersion, InstallClient.VersionType type, String url) {

    }

    public record BuildScript(List<BuildCommand> script, String result, String path) {

    }

    public record BuildCommand(String workdir, List<String> cmd) {

    }
}

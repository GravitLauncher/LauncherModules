package pro.gravit.launchermodules.mirrorhelper;

import pro.gravit.launcher.base.profiles.ClientProfile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MirrorWorkspace(int schemaVersion, Map<InstallClient.VersionType, Loader> loaders,
                              Lwjgl lwjgl, List<Library> artifacts,
                              Map<String, MultiMod> clientArtifacts, Map<String, BuildScript> builds) {
    public MirrorWorkspace {
        loaders = loaders == null ? Map.of() : Map.copyOf(loaders);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        clientArtifacts = clientArtifacts == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(clientArtifacts));
        builds = builds == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(builds));
    }

    public List<String> mods(InstallClient.VersionType type) {
        Loader loader = loaders.get(type);
        return loader == null || loader.mods() == null ? List.of() : loader.mods();
    }

    public String loaderVersion(InstallClient.VersionType type) {
        Loader loader = loaders.get(type);
        return loader == null ? null : loader.version();
    }

    public String lwjglVersion(ClientProfile.Version version) {
        String result = lwjgl.defaultVersion();
        for (var override : lwjgl.overrides()) {
            if (override.when() == null || override.when().matches(null, version)) {
                result = override.version();
            }
        }
        return result;
    }

    public record Loader(String version, List<String> mods) {}
    public record Lwjgl(String defaultVersion, List<LwjglOverride> overrides) {
        public Lwjgl {
            overrides = overrides == null ? List.of() : List.copyOf(overrides);
        }
    }
    public record LwjglOverride(Selector when, String version) {}

    public record Selector(List<InstallClient.VersionType> loaders, ClientProfile.Version minVersion,
                           ClientProfile.Version maxVersion) {
        boolean matches(InstallClient.VersionType loader, ClientProfile.Version version) {
            return (loaders == null || loaders.isEmpty() || loaders.contains(loader))
                    && (minVersion == null || version.compareTo(minVersion) >= 0)
                    && (maxVersion == null || version.compareTo(maxVersion) <= 0);
        }
    }

    public record Library(String path, String url, String data, Map<String, String> unpack, List<String> prefixFilter) {}

    public record MultiMod(Selector when, String url, String target) {
        boolean check(InstallClient.VersionType type, ClientProfile.Version version) {
            return when == null || when.matches(type, version);
        }
    }

    public record BuildScript(List<BuildCommand> script, String result, String path, Selector when, boolean dynamic) {
        boolean check(InstallClient.VersionType type, ClientProfile.Version version) {
            return when == null || when.matches(type, version);
        }
    }

    public record BuildCommand(String workdir, List<String> cmd, boolean ignoreErrorCode, Map<String, String> env) {}
}

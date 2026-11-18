package pro.gravit.launchermodules.mirrorhelper;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class WorkspaceManifest {
    private WorkspaceManifest() {}

    static MirrorWorkspace read(Path path, boolean local) throws IOException {
        try (var reader = IOHelper.newReader(path)) {
            JsonElement json = JsonParser.parseReader(reader);
            var root = json.getAsJsonObject();
            if (!root.has("schemaVersion") || !root.get("schemaVersion").toString().equals("2")) {
                throw new IllegalArgumentException("schemaVersion must be 2. Apply a v2 workspace.");
            }
            fields(json, "schemaVersion", "loaders", "lwjgl", "artifacts", "clientArtifacts", "builds");
            if (root.has("loaders")) {
                root.getAsJsonObject("loaders").asMap().forEach((name, loader) -> {
                    InstallClient.VersionType.valueOf(name);
                    fields(loader, "version", "mods");
                });
            }
            fields(root.get("lwjgl"), "defaultVersion", "overrides");
            var lwjgl = root.getAsJsonObject("lwjgl");
            if (lwjgl.has("overrides")) {
                for (var override : lwjgl.getAsJsonArray("overrides")) {
                    fields(override, "when", "version");
                    selectorFields(override);
                }
            }
            if (root.has("artifacts")) {
                for (var artifact : root.getAsJsonArray("artifacts")) {
                    fields(artifact, "path", "url", "data", "unpack", "prefixFilter");
                }
            }
            if (root.has("clientArtifacts")) {
                for (var artifact : root.getAsJsonObject("clientArtifacts").asMap().values()) {
                    fields(artifact, "when", "url", "target");
                    selectorFields(artifact);
                }
            }
            if (root.has("builds")) {
                var builds = root.getAsJsonObject("builds");
                if (!local && builds.size() != 0) {
                    throw new IllegalArgumentException("builds are allowed only in local workspace manifests");
                }
                for (var build : builds.asMap().values()) {
                    fields(build, "when", "script", "result", "path", "dynamic");
                    selectorFields(build);
                    for (var step : build.getAsJsonObject().getAsJsonArray("script")) {
                        fields(step, "workdir", "cmd", "ignoreErrorCode", "env");
                    }
                }
            }
            var workspace = Launcher.gsonManager.gson.fromJson(json, MirrorWorkspace.class);
            validate(workspace);
            return workspace;
        } catch (RuntimeException e) {
            throw new IOException("Invalid workspace manifest " + path + ": " + e.getMessage(), e);
        }
    }

    private static void fields(JsonElement element, String... names) {
        if (element == null) {
            throw new IllegalArgumentException("Missing object with fields " + String.join(", ", names));
        }
        var allowed = Set.of(names);
        for (var name : element.getAsJsonObject().keySet()) {
            if (!allowed.contains(name)) {
                throw new IllegalArgumentException("Unknown workspace field " + name);
            }
        }
    }

    private static void selectorFields(JsonElement element) {
        if (element.getAsJsonObject().has("when")) {
            var selector = element.getAsJsonObject().get("when");
            fields(selector, "loaders", "minVersion", "maxVersion");
            for (String bound : Set.of("minVersion", "maxVersion")) {
                var value = selector.getAsJsonObject().get(bound);
                if (value != null && (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                        || !value.getAsString().matches("[0-9]+(\\.[0-9]+)*"))) {
                    throw new IllegalArgumentException("when." + bound + " must be a dotted numeric version");
                }
            }
            if (selector.getAsJsonObject().has("loaders")) {
                for (var loader : selector.getAsJsonObject().getAsJsonArray("loaders")) {
                    InstallClient.VersionType.valueOf(loader.getAsString());
                }
            }
        }
    }

    private static void validate(MirrorWorkspace workspace) {
        Set<Path> destinations = new HashSet<>();
        component(workspace.lwjgl().defaultVersion());
        for (var loader : workspace.loaders().values()) {
            if (loader.version() != null) {
                component(loader.version());
            }
        }
        for (var override : workspace.lwjgl().overrides()) {
            component(override.version());
            validateSelector(override.when());
            if (override.when() != null && override.when().loaders() != null) {
                throw new IllegalArgumentException("lwjgl.overrides selectors only accept version bounds");
            }
        }
        for (var artifact : workspace.artifacts()) {
            if ((artifact.url() == null) == (artifact.data() == null)) {
                throw new IllegalArgumentException("artifacts require exactly one of url or data");
            }
            if (artifact.url() != null) {
                remoteUri(artifact.url());
            }
            if (artifact.path() != null) {
                destination(destinations, artifact.path());
            }
            if (artifact.unpack() != null) {
                artifact.unpack().values().forEach(path -> destination(destinations, path));
            }
            if ((artifact.path() == null && (artifact.data() != null || artifact.unpack() == null || artifact.unpack().isEmpty()))
                    || (artifact.data() != null && (artifact.unpack() != null || artifact.prefixFilter() != null))
                    || (artifact.prefixFilter() != null && artifact.path() == null)) {
                throw new IllegalArgumentException("Invalid artifact outputs for " + artifact.path());
            }
        }
        workspace.clientArtifacts().forEach((id, artifact) -> {
            component(id);
            remoteUri(artifact.url());
            if (artifact.target() != null) {
                relative(artifact.target());
            }
            validateSelector(artifact.when());
        });
        workspace.builds().forEach((id, build) -> {
            component(id);
            validateSelector(build.when());
            if (build.dynamic()) {
                relative(build.path());
            } else {
                destination(destinations, build.path());
            }
            if (build.result() == null || build.script().isEmpty() || build.script().stream().anyMatch(step -> step.cmd() == null || step.cmd().isEmpty())) {
                throw new IllegalArgumentException("Invalid builds entry " + id);
            }
        });
    }

    private static void validateSelector(MirrorWorkspace.Selector selector) {
        if (selector != null && selector.minVersion() != null && selector.maxVersion() != null
                && selector.minVersion().compareTo(selector.maxVersion()) > 0) {
            throw new IllegalArgumentException("when.minVersion exceeds when.maxVersion");
        }
    }

    private static void destination(Set<Path> destinations, String value) {
        Path path = relative(value);
        String first = path.getName(0).toString();
        if (!Set.of("authlib", "installers", "workdir").contains(first) || path.getNameCount() < 2
                || path.startsWith(Path.of("workdir", "lwjgl"))
                || destinations.stream().anyMatch(other -> other.startsWith(path) || path.startsWith(other))) {
            throw new IllegalArgumentException("Conflicting or reserved artifacts destination " + value);
        }
        destinations.add(path);
    }

    public static URI remoteUri(String value) {
        URI uri = URI.create(value);
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Expected an HTTP(S) URL without credentials");
        }
        return uri;
    }

    public static String component(String value) {
        if (relative(value).getNameCount() != 1 || value.contains("/")) {
            throw new IllegalArgumentException("Expected a single path component: " + value);
        }
        return value;
    }

    private static Path relative(String value) {
        if (value == null || value.isBlank() || value.contains("\\") || value.contains(":")) {
            throw new IllegalArgumentException("Invalid relative path: " + value);
        }
        Path path = Path.of(value);
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("Expected a relative path: " + value);
        }
        for (Path part : path) {
            if (part.toString().equals("..") || part.toString().equals(".")) {
                throw new IllegalArgumentException("Invalid relative path: " + value);
            }
        }
        return path;
    }

    public static Path resolve(Path root, String value) throws IOException {
        Path path = root;
        if (Files.isSymbolicLink(root)) {
            throw new IOException("Unexpected symbolic link: " + root);
        }
        for (Path part : relative(value)) {
            path = path.resolve(part);
            if (Files.isSymbolicLink(path)) {
                throw new IOException("Unexpected symbolic link: " + path);
            }
        }
        return path;
    }
}

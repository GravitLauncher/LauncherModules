package pro.gravit.launchermodules.mirrorhelper.installers;

import pro.gravit.launcher.base.Launcher;
import pro.gravit.launchermodules.mirrorhelper.WorkspaceManifest;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class LoaderLibraries {
    private LoaderLibraries() {}

    public static Artifact resolve(String url, String coordinate) {
        String[] qualified = coordinate.split("@", -1);
        String[] parts = qualified[0].split(":", -1);
        if (qualified.length > 2 || parts.length < 3 || parts.length > 4) {
            throw new IllegalArgumentException("Invalid Maven coordinate: " + coordinate);
        }
        for (String group : parts[0].split("\\.", -1)) {
            WorkspaceManifest.component(group);
        }
        Arrays.stream(parts, 1, parts.length).forEach(WorkspaceManifest::component);
        String classifier = parts.length == 4 ? "-" + parts[3] : "";
        String extension = qualified.length == 2 ? WorkspaceManifest.component(qualified[1]) : "jar";
        String path = "%s/%s/%s/%s-%s%s.%s".formatted(
                parts[0].replace('.', '/'), parts[1], parts[2], parts[1], parts[2], classifier, extension);
        URI uri = WorkspaceManifest.remoteUri(url);
        return new Artifact(url.endsWith("/") ? uri.resolve(path) : uri, path);
    }

    static void install(Path client, List<String> command) throws Exception {
        if (!Files.isDirectory(client)) {
            throw new IOException("Client directory missing: " + client);
        }
        Process process = new ProcessBuilder(command).inheritIO().start();
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Loader installer exited with code " + process.exitValue());
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        }
        Path versions = client.resolve("versions");
        Path profile;
        try (var paths = Files.find(versions, 2, (path, attributes) ->
                attributes.isRegularFile() && path.getFileName().toString().endsWith(".json"))) {
            profile = paths.sorted().findFirst().orElseThrow(() -> new IOException("Installer did not create a profile"));
        }
        try (var reader = IOHelper.newReader(profile)) {
            var metadata = Launcher.gsonManager.configGson.fromJson(reader, Profile.class);
            for (var library : metadata.libraries()) {
                var artifact = resolve(library.url(), library.name());
                var target = WorkspaceManifest.resolve(client, "libraries/" + artifact.path());
                try (var input = IOHelper.newInput(artifact.uri().toURL())) {
                    IOHelper.transfer(input, target);
                }
            }
        }
        IOHelper.deleteDir(versions, true);
    }

    public record Artifact(URI uri, String path) {}
    private record Library(String name, String url) {}
    private record Profile(List<Library> libraries) {}
}

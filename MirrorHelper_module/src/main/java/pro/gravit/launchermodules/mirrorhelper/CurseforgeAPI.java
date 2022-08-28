package pro.gravit.launchermodules.mirrorhelper;

import com.google.gson.JsonElement;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchserver.helper.HttpHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class CurseforgeAPI {
    private static final String BASE_URL = "https://api.curseforge.com";
    private final String apiKey;

    public CurseforgeAPI(String apiKey) {
        this.apiKey = apiKey;
    }

    private final HttpClient client = HttpClient.newBuilder().build();
    public Mod fetchModById(long id) throws IOException, URISyntaxException {
        return HttpHelper.send(client, HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI(BASE_URL+"/v1/mods/"+ id))
                        .header("Accept", "application/json")
                        .header("x-api-key", apiKey)
                .build(), new CurseForgeErrorHandler<>(Mod.class)).getOrThrow();
    }

    public String fetchModDescriptionById(long id) throws IOException, URISyntaxException {
        return HttpHelper.send(client, HttpRequest.newBuilder()
                .GET()
                .uri(new URI(BASE_URL+"/v1/mods/"+ id +"/description"))
                .header("Accept", "application/json")
                .header("x-api-key", apiKey)
                .build(), new CurseForgeErrorHandler<>(String.class)).getOrThrow();
    }

    public Artifact fetchModFileById(long modId, long fileId) throws IOException, URISyntaxException {
        return HttpHelper.send(client, HttpRequest.newBuilder()
                .GET()
                .uri(new URI(BASE_URL+"/v1/mods/"+ modId + "/files/" + fileId))
                .header("Accept", "application/json")
                .header("x-api-key", apiKey)
                .build(), new CurseForgeErrorHandler<>(Artifact.class)).getOrThrow();
    }

    public String fetchModFileUrlById(long modId, long fileId) throws IOException, URISyntaxException {
        return HttpHelper.send(client, HttpRequest.newBuilder()
                .GET()
                .uri(new URI(BASE_URL+"/v1/mods/"+ modId + "/files/" + fileId + "/download-url"))
                .header("Accept", "application/json")
                .header("x-api-key", apiKey)
                .build(), new CurseForgeErrorHandler<>(String.class)).getOrThrow();
    }

    public static class CurseForgeErrorHandler<T> implements HttpHelper.HttpErrorHandler<T, Void> {
        private final Class<T> type;

        public CurseForgeErrorHandler(Class<T> type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, Void> apply(HttpResponse<InputStream> response) {
            if(response.statusCode() < 200 || response.statusCode() >= 300) {
                return new HttpHelper.HttpOptional<>(null, null, response.statusCode());
            }
            try(Reader reader = new InputStreamReader(response.body())) {
                JsonElement element = Launcher.gsonManager.gson.fromJson(reader, JsonElement.class);
                return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(element.getAsJsonObject().get("data"), type), null, response.statusCode());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public record SortableGameVersion(String gameVersionName, String gameVersionPadded, String gameVersion, String gameVersionReleaseDate, int gameVersionTypeId) {

    }
    public record ModDependency(long modId, int relationType) {

    }
    public record Artifact(long id, long gameId, long modId, String displayName, String fileName, int releaseType, String downloadUrl, List<String> gameVersions, List<SortableGameVersion> sortableGameVersions,
                           List<ModDependency> dependencies, long alternateFileId, boolean isServerPack, long fileFingerprint) {

    }
    public record ArtifactIndex(String gameVersion, long fileId, String filename, int releaseType, int gameVersionTypeId, Integer modLoader) {

    }
    public record Mod(long id, long gameId, String name,
                      long mainFileId, List<Artifact> latestFiles, List<ArtifactIndex> latestFilesIndexes) {
        public long findFileIdByGameVersion(String version) {
            for(var e : latestFilesIndexes) {
                if(e.gameVersion.equals(version)) {
                    return e.fileId;
                }
            }
            throw new RuntimeException(String.format("Mod '%s' not supported game version '%s'", name, version));
        }
    }
}

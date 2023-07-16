package pro.gravit.launchermodules.mirrorhelper.modapi;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import pro.gravit.launcher.Launcher;
import pro.gravit.launchermodules.mirrorhelper.MirrorHelperModule;
import pro.gravit.launchserver.helper.HttpHelper;
import pro.gravit.utils.Version;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ModrinthAPI {
    private static final String BASE_URL = "https://api.modrinth.com/v2/";
    private static final String userAgent = "GravitLauncher/%s MirrorHelper/%s".formatted(Version.getVersion().getVersionString(), MirrorHelperModule.version.getVersionString());

    private String apiKey;
    public final HttpClient client = HttpClient.newBuilder().build();

    public ModrinthAPI(String apiKey) {
        this.apiKey = apiKey;
    }

    public ModrinthAPI() {
    }

    @SuppressWarnings("unchecked")
    public List<ModVersionData> getMod(String slug) throws IOException {
        TypeToken<List<ModVersionData>> typeToken = new TypeToken<>(){};
        return (List<ModVersionData>) HttpHelper.send(client, HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL.concat("project/%s/version".formatted(slug))))
                .header("User-Agent", userAgent)
                .build(), new ModrinthErrorHandler<>(typeToken.getType())).getOrThrow();
    }

    public ModVersionData getModByGameVersion(List<ModVersionData> list, String gameVersion, String loader) {
        for(var e : list) {
            if(!e.loaders.contains(loader)) {
                continue;
            }
            if(!e.game_versions.contains(gameVersion)) {
                continue;
            }
            return e;
        }
        return null;
    }

    public record ModVersionData(String id, String name, List<ModVersionFileData> files, List<String> game_versions, List<String> loaders) {

    }

    public record ModVersionFileData(Map<String, String> hashes, String url, String filename, boolean primary) {

    }

    public static class ModrinthErrorHandler<T> implements HttpHelper.HttpErrorHandler<T, Void> {
        private final Type type;

        public ModrinthErrorHandler(Type type) {
            this.type = type;
        }

        @Override
        public HttpHelper.HttpOptional<T, Void> apply(HttpResponse<InputStream> response) {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new HttpHelper.HttpOptional<>(null, null, response.statusCode());
            }
            try (Reader reader = new InputStreamReader(response.body())) {
                JsonElement element = Launcher.gsonManager.gson.fromJson(reader, JsonElement.class);
                return new HttpHelper.HttpOptional<>(Launcher.gsonManager.gson.fromJson(element, type), null, response.statusCode());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

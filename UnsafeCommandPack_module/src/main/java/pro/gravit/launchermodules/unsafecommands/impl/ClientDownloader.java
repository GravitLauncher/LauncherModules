package pro.gravit.launchermodules.unsafecommands.impl;

import com.google.gson.*;
import pro.gravit.utils.helper.IOHelper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ClientDownloader {
    public static final Gson GSON = new GsonBuilder().setLenient().create();

    public static JsonObject gainClient(String mc) throws IOException {
        try {
            String workURL = null;
            JsonObject obj = GSON.fromJson(
                    IOHelper.request(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json")),
                    JsonObject.class);
            if (obj.has("versions") && obj.get("versions").isJsonArray())
                for (JsonElement el : obj.get("versions").getAsJsonArray())
                    if (el != null && el.isJsonObject()) {
                        JsonElement ver = el.getAsJsonObject().get("id");
                        if (ver != null && ver.isJsonPrimitive() && ver.getAsJsonPrimitive().isString()
                                && mc.equals(ver.getAsJsonPrimitive().getAsString())
                                && el.getAsJsonObject().has("url") && el.getAsJsonObject().get("url").isJsonPrimitive()
                                && el.getAsJsonObject().get("url").getAsJsonPrimitive().isString())
                            workURL = el.getAsJsonObject().get("url").getAsString();
                    }
            if (workURL != null) {
                obj = GSON.fromJson(IOHelper.request(new URL(workURL)), JsonObject.class);
                return obj;
            }
            throw new IOException("Client not found");
        } catch (JsonSyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClientInfo getClient(JsonObject obj) throws IOException {
        JsonArray libraries = obj.getAsJsonArray("libraries");
        ClientInfo ret = new ClientInfo();
        for (JsonElement e : libraries) {
            if (e.isJsonObject() && e.getAsJsonObject().has("downloads")) {
                JsonObject downloads = e.getAsJsonObject().getAsJsonObject("downloads");
                if (downloads.has("classifiers")) {
                    JsonObject u = downloads.getAsJsonObject("classifiers");
                    u.entrySet().forEach(p -> {
                        if (p.getValue().isJsonObject() && p.getKey().startsWith("native")) {
                            Artifact a = GSON.fromJson(p.getValue(), Artifact.class);
                            a.name = p.getKey() + '/' + e.getAsJsonObject().get("name").getAsString();
                            ret.natives.add(a);
                        }
                    });
                } else if (downloads.has("artifact")) {
                    Artifact a = GSON.fromJson(downloads.get("artifact"), Artifact.class);
                    a.name = "art/" + e.getAsJsonObject().get("name").getAsString();
                    ret.libraries.add(a);
                }

            }
        }
        if (obj.has("downloads")) {
            JsonObject tmp = obj.getAsJsonObject("downloads");
            ret.client = GSON.fromJson(tmp.get("client"), Downloadable.class);
            ret.server = GSON.fromJson(tmp.get("server"), Downloadable.class);
        }
        dedupe(ret.libraries);
        dedupe(ret.natives);
        return ret;
    }

    public static void dedupe(List<Artifact> lst) {
        // currently empty
    }

    public static class Downloadable {
        public String sha1;
        public String url;
        public int size;
        public transient String name;
    }

    public static class ClientInfo {
        public Downloadable server, client;
        public List<Artifact> libraries = new ArrayList<>();
        public List<Artifact> natives = new ArrayList<>();
    }

    public static class Artifact extends Downloadable {
        public String path;
    }
}

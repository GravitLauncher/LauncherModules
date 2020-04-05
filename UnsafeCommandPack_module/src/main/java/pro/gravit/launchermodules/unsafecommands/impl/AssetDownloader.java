package pro.gravit.launchermodules.unsafecommands.impl;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.utils.helper.IOHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

public class AssetDownloader {
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private AssetDownloader() {
    }

    public static String gainAssetsURL(String mc) throws IOException {
        try {
            JsonObject obj = ClientDownloader.gainClient(mc);
            if (obj.has("assetIndex") && obj.getAsJsonObject("assetIndex").has("url"))
                return obj.getAsJsonObject("assetIndex").get("url").getAsString();
            throw new IOException("Assets not found");
        } catch (JsonSyntaxException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getBase() {
        return "https://resources.download.minecraft.net/";
    }

    public static List<AsyncDownloader.SizedFile> listAssets(Path loc, String ver) throws IOException {
        List<AsyncDownloader.SizedFile> applies = new ArrayList<>();
        String dIndex = "indexes/" + ver + ".json";
        String assetsURL = gainAssetsURL(ver);
        Path indexPath = loc.resolve(dIndex);
        AssetIndex index;
        try (BufferedReader in = IOHelper.newReader(new URL(assetsURL))) {
            index = GSON.fromJson(in, AssetIndex.class);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        try (BufferedWriter out = IOHelper.newWriter(indexPath)) {
            GSON.toJson(index, AssetIndex.class, out);
        } catch (JsonIOException e) {
            if (e.getCause() != null)
                throw (IOException) e.getCause();
            else
                throw new RuntimeException(e);
        }
        if (index != null)
            for (AssetObject assetObject : index.objects.values()) {
                String dest = assetObject.getLocation();
                applies.add(new AsyncDownloader.SizedFile(dest, "objects/" + dest, assetObject.size));
            }
        return applies;
    }

    public static final class AssetIndex {

        @SerializedName("virtual")
        public boolean virtual;

        @SerializedName("objects")
        public Map<String, AssetObject> objects;

        public AssetIndex() {
            this(false, Collections.emptyMap());
        }

        public AssetIndex(boolean virtual, Map<String, AssetObject> objects) {
            this.virtual = virtual;
            this.objects = new HashMap<>(objects);
        }
    }

    public static final class AssetObject {

        @SerializedName("hash")
        public String hash;
        @SerializedName("size")
        public long size;

        public AssetObject() {
            this("", 0);
        }

        public AssetObject(String hash, long size) {
            this.hash = hash;
            this.size = size;
        }

        public String getLocation() {
            return hash.substring(0, 2) + "/" + hash;
        }
    }
}
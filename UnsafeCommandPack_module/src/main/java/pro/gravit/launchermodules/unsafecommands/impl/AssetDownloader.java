package pro.gravit.launchermodules.unsafecommands.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import pro.gravit.launcher.AsyncDownloader;
import pro.gravit.utils.helper.IOHelper;

public class AssetDownloader {
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

	public static class DownloadInfo {
		@SerializedName("totalSize")
		public long totalSize;
		@SerializedName("url")
		public String url;
		@SerializedName("sha1")
		public String sha1;
		@SerializedName("size")
		public int size;
		@SerializedName("id")
		public String id;

		public DownloadInfo() {
			this("", "");
		}

		public DownloadInfo(String id, String url) {
			this(id, url, null);
		}

		public DownloadInfo(String id, String url, String sha1) {
			this(id, url, sha1, 0);
		}

		public DownloadInfo(String id, String url, String sha1, int size) {
			this(id, url, sha1, size, 0);
		}

		public DownloadInfo(String id, String url, String sha1, int size, long totalSize) {
			this.url = url;
			this.sha1 = sha1;
			this.size = size;
			this.id = id;
			this.totalSize = totalSize;
		}
	}

	private static final Gson GSON = new GsonBuilder().setLenient().create();

	public static String gainAssetsURL(String mc) throws IOException {
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
				if (obj.has("assetIndex") && obj.get("assetIndex").isJsonObject())
					return GSON.fromJson(obj.get("assetIndex"), DownloadInfo.class).url;
			}
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
				applies.add(new AsyncDownloader.SizedFile("objects/" + dest, dest, assetObject.size));
			}
		return applies;
	}

	private AssetDownloader() {
	}
}
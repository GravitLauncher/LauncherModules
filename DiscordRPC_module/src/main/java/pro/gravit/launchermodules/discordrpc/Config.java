package pro.gravit.launchermodules.discordrpc;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

import com.google.gson.annotations.SerializedName;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

class Config {
	@SerializedName("appId")
    String appId;
	@SerializedName("firstLine")
    String firstLine = "Играет на %profile%";
	@SerializedName("secondLine")
    String secondLine = "Ник: %user%";
	@SerializedName("largeKey")
    String largeKey = "large.png";
	@SerializedName("smallKey")
    String smallKey = "small.png";
	@SerializedName("largeText")
    String largeText = "Everything";
	@SerializedName("smallText")
    String smallText = "Everything";
	
	static Config read(Reader r) {
        return r != null ? DiscordRPC.confGson.fromJson(r, Config.class) : new Config();
	}

	static Config getOrCreate(Path f) {
		try {
			if (IOHelper.exists(f))
				try (Reader r = IOHelper.newReader(f)) {
					return read(r);
				}
			else {
				Config ret = new Config();
				try (Writer w = IOHelper.newWriter(f)) {
					ret.write(w);
				}
				return ret;
			}
		} catch (Exception ex) {
			LogHelper.error(ex);
			return new Config();
		}
	}
	
	void write(Writer w) {
		DiscordRPC.confGson.toJson(this, Config.class, w);
	}
}

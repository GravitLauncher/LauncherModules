package pro.gravit.launchermodules.discordrpc;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

import com.google.gson.annotations.SerializedName;

import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class Config {
	@SerializedName("appId")
    public String appId;
	@SerializedName("firstLine")
    public String firstLine = "Играет на %profile%";
	@SerializedName("secondLine")
    public String secondLine = "Ник: %user%";
	@SerializedName("largeKey")
    public String largeKey = "large.png";
	@SerializedName("smallKey")
    public String smallKey = "small.png";
	@SerializedName("largeText")
    public String largeText = "Everything";
	@SerializedName("smallText")
    public String smallText = "Everything";
	
	public static Config read(Reader r) {
        return r != null ? DiscordRPC.confGson.fromJson(r, Config.class) : new Config();
	}

	public static Config getOrCreate(Path f) {
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
	
	public void write(Writer w) {
		DiscordRPC.confGson.toJson(this, Config.class, w);
	}
}

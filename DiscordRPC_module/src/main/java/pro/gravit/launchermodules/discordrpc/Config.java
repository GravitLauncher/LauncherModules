package pro.gravit.launchermodules.discordrpc;

import com.google.gson.annotations.SerializedName;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

public class Config {
    @SerializedName("appId")
    public String appId;
    @SerializedName("firstLine")
    public String firstLine = "Играет на %profile%";
    @SerializedName("secondLine")
    public String secondLine = "Ник: %user%";
    @SerializedName("largeKey")
    public final String largeKey = "large";
    @SerializedName("smallKey")
    public final String smallKey = "small";
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

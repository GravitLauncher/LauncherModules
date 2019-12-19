package pro.gravit.launchermodules.sentryl;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

public class Config {
    static final Gson confGson = CommonHelper.newBuilder().setPrettyPrinting().serializeNulls().create();
    @SerializedName("dsn")
    public String dsn = "YOUR_DSN";
    @SerializedName("captureAll")
    public boolean captureAll = false;

    public static Config read(Reader r) {
        return r != null ? confGson.fromJson(r, Config.class) : new Config();
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
        confGson.toJson(this, Config.class, w);
    }
}

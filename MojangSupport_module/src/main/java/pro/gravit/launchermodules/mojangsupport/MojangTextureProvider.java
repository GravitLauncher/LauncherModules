package pro.gravit.launchermodules.mojangsupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pro.gravit.launcher.HTTPRequest;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launchserver.auth.texture.TextureProvider;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.utils.helper.VerifyHelper;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MojangTextureProvider extends TextureProvider {
    public static final long CACHE_DURATION_MS = VerifyHelper.verifyLong(
            Long.parseLong(System.getProperty("launcher.mysql.cacheDurationHours", Integer.toString(24))),
            VerifyHelper.L_NOT_NEGATIVE, "launcher.mysql.cacheDurationHours can't be < 0") * 60L * 60L * 1000L;
    // Instance
    private final Map<String, CacheData> cache = new HashMap<>(1024);

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public synchronized Texture getCloakTexture(UUID uuid, String username, String client) {
        return getCached(uuid, username).cloak;
    }

    @Override
    public synchronized Texture getSkinTexture(UUID uuid, String username, String client) {
        return getCached(uuid, username).skin;
    }

    private CacheData getCached(UUID uuid, String username) {
        CacheData result = cache.get(username);

        // Have cached result?
        if (result != null && System.currentTimeMillis() < result.until) {
            if (result.exc != null) {
                throw new RuntimeException(result.exc);
            }
            return result;
        }

        try {
            String uuidResolved = uuid.toString().replaceAll("-", "");

            // Obtain player profile
            URL profileURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidResolved);
            JsonElement profileResponse = HTTPRequest.jsonRequest(null, "GET", profileURL);
            if (profileResponse == null) {
                throw new IllegalArgumentException("Empty Mojang response");
            }
            JsonArray properties = (JsonArray) profileResponse.getAsJsonObject().get("properties");
            if (properties == null) {
                LogHelper.subDebug("No properties");
                return cache(username, null, null, null);
            }

            // Find textures property
            JsonObject texturesProperty = null;
            for (JsonElement property : properties) {
                JsonObject property0 = property.getAsJsonObject();
                if (property0.get("name").getAsString().equals("textures")) {
                    byte[] asBytes = Base64.getDecoder().decode(property0.get("value").getAsString());
                    texturesProperty = JsonParser.parseString(new String(asBytes, StandardCharsets.UTF_8)).getAsJsonObject();
                    break;
                }
            }
            if (texturesProperty == null) {
                LogHelper.subDebug("No textures property");
                return cache(username, null, null, null);
            }

            // Extract skin&cloak texture
            texturesProperty = (JsonObject) texturesProperty.get("textures");
            JsonObject skinProperty = (JsonObject) texturesProperty.get("SKIN");
            Texture skinTexture = skinProperty == null ? null : new Texture(skinProperty.get("url").getAsString(), false);
            JsonObject cloakProperty = (JsonObject) texturesProperty.get("CAPE");
            Texture cloakTexture = cloakProperty == null ? null : new Texture(cloakProperty.get("url").getAsString(), true);

            // We're done
            return cache(username, skinTexture, cloakTexture, null);
        } catch (Throwable exc) {
            cache(username, null, null, exc);
            throw new RuntimeException(exc);
        }

        // We're dones
    }

    private CacheData cache(String username, Texture skin, Texture cloak, Throwable exc) {
        long until = CACHE_DURATION_MS == 0L ? Long.MIN_VALUE : System.currentTimeMillis() + CACHE_DURATION_MS;
        CacheData data = exc == null ? new CacheData(skin, cloak, until) : new CacheData(exc, until);
        if (CACHE_DURATION_MS != 0L) {
            cache.put(username, data);
        }
        return data;
    }

    public static class EmptyObject {
        public boolean legacy = true;
    }

    private static final class CacheData {
        private final Texture skin, cloak;
        private final Throwable exc;
        private final long until;

        private CacheData(Texture skin, Texture cloak, long until) {
            this.skin = skin;
            this.cloak = cloak;
            this.until = until;
            exc = null;
        }

        private CacheData(Throwable exc, long until) {
            this.exc = exc;
            this.until = until;
            skin = cloak = null;
        }
    }
}
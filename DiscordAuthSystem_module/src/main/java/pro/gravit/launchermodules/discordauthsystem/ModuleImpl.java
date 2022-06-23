package pro.gravit.launchermodules.discordauthsystem;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.config.JsonConfigurable;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.PreConfigPhase;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordApi;
import pro.gravit.launchermodules.discordauthsystem.providers.DiscordSystemAuthCoreProvider;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.modules.events.LaunchServerFullInitEvent;
import pro.gravit.launchserver.modules.impl.LaunchServerInitContext;
import pro.gravit.launchserver.socket.handlers.NettyWebAPIHandler;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 0, Version.Type.LTS);
    private static boolean registred = false;
    private final transient Logger logger = LogManager.getLogger();
    public JsonConfigurable<Config> jsonConfigurable;
    public Config config;

    public ModuleImpl() {
        super(new LauncherModuleInfo("DiscordAuthSystem", version, new String[]{"LaunchServerCore"}));
    }

    public void preConfig(PreConfigPhase preConfigPhase) {
        if (!registred) {
            AuthCoreProvider.providers.register("discordauthsystem", DiscordSystemAuthCoreProvider.class);
            registred = true;
        }
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preConfig, PreConfigPhase.class);
        registerEvent(this::finish, LaunchServerFullInitEvent.class);
        jsonConfigurable = modulesConfigManager.getConfigurable(Config.class, moduleInfo.name);
    }

    public void finish(LaunchServerFullInitEvent event) {
        LaunchServer launchServer = event.server;
        try {
            jsonConfigurable.loadConfig();
            config = jsonConfigurable.getConfig();
        } catch (IOException e) {
            LogHelper.error(e);
        }
        DiscordApi.initialize(config);
        NettyWebAPIHandler.addNewSeverlet("auth/discord", new WebApi(this, launchServer));
    }

//    public void exit(ClosePhase closePhase) {
//        if (jsonConfigurable != null && jsonConfigurable.getConfig() != null)
//            save();
//    }
//
//    public void load() {
//        load(dbPath);
//    }
//
//    public void load(Path path) {
//        {
//            Path sessionsPath = path.resolve("Sessions.json");
//            if (!Files.exists(sessionsPath)) return;
//            Type sessionsType = new TypeToken<Set<DiscordSystemAuthCoreProvider.DiscordUserSession>>() {
//            }.getType();
//            try (Reader reader = IOHelper.newReader(sessionsPath)) {
//                this.sessions = Launcher.gsonManager.configGson.fromJson(reader, sessionsType);
//            } catch (IOException e) {
//                LogHelper.error(e);
//            }
//            for (DiscordSystemAuthCoreProvider.DiscordUserSession sessionEntity : sessions) {
//                if (sessionEntity.userEntityUUID != null) {
//                    sessionEntity.user = getUserByUUID(sessionEntity.userEntityUUID);
//                }
//            }
//        }
//    }
//
//    public void save() {
//        save(dbPath);
//    }
//
//    public void save(Path path) {
//        {
//            Path sessionsPath = path.resolve("Sessions.json");
//            Type sessionsType = new TypeToken<Set<DiscordSystemAuthCoreProvider.DiscordUserSession>>() {
//            }.getType();
//            try (Writer writer = IOHelper.newWriter(sessionsPath)) {
//                Launcher.gsonManager.configGson.toJson(sessions, sessionsType, writer);
//            } catch (IOException e) {
//                LogHelper.error(e);
//            }
//        }
//    }
//
//    public void preConfig(PreConfigPhase preConfigPhase) {
//        AuthCoreProvider.providers.register("discordauthsystem", DiscordSystemAuthCoreProvider.class);
//    }

//    public DiscordSystemAuthCoreProvider.DiscordUser getUser(JoinServerRequest request) {
//        JsonElement responseUsername;
//        JsonElement responseUUID;
//        request.parameters = config.addParameters;
//        try {
//            JsonElement r = HTTPRequest.jsonRequest(gson.toJsonTree(request), new URL(config.backendUserUrl));
//            if (r == null) {
//                return null;
//            }
//            JsonObject response = r.getAsJsonObject();
//            responseUsername = response.get("username");
//            responseUUID = response.get("uuid");
//        } catch (IllegalStateException | IOException ignore) {
//            return null;
//        }
//        if (responseUsername != null && responseUUID != null) {
//            return new DiscordSystemAuthCoreProvider.DiscordUser(responseUsername.getAsString(), UUID.fromString(responseUUID.getAsString()));
//        } else {
//            return null;
//        }
//    }

//    public DiscordSystemAuthCoreProvider.DiscordUserSession getSessionByAccessToken(String accessToken) {
//        return sessions.stream().filter(e -> e.accessToken != null && e.accessToken.equals(accessToken)).findFirst().orElse(null);
//    }

//    public DiscordSystemAuthCoreProvider.DiscordUserSession getSessionByRefreshToken(String refreshToken) {
//        return sessions.stream().filter(e -> e.accessToken != null && e.refreshToken.equals(refreshToken)).findFirst().orElse(null);
//    }

//    public boolean deleteSession(DiscordSystemAuthCoreProvider.DiscordUserSession entity) {
//        return sessions.remove(entity);
//    }

//    public boolean exitUser(DiscordSystemAuthCoreProvider.DiscordUser user) {
//        return sessions.removeIf(e -> e.user == user);
//    }
}
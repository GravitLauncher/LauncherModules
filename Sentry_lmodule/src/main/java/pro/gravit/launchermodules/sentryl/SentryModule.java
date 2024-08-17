package pro.gravit.launchermodules.sentryl;

import io.sentry.IScopes;
import io.sentry.Scope;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import pro.gravit.launcher.base.Launcher;
import pro.gravit.launcher.base.api.AuthService;
import pro.gravit.launcher.runtime.LauncherEngine;
import pro.gravit.launcher.runtime.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.ClientProcessInitPhase;
import pro.gravit.launcher.client.events.ClientProcessPreInvokeMainClassEvent;
import pro.gravit.launcher.core.LauncherInject;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.modules.LauncherModuleInfo;
import pro.gravit.launcher.base.profiles.ClientProfile;
import pro.gravit.launcher.base.profiles.PlayerProfile;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launchermodules.sentryl.utils.BasicProperties;
import pro.gravit.launchermodules.sentryl.utils.OshiUtils;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.util.HashMap;
import java.util.Map;

public class SentryModule extends LauncherModule {

    public static Config config = new Config();

    public static IScopes currentScopes;

    @LauncherInject("modules.sentry.proguarduuid")
    public static String proguardUuid;

    public SentryModule() {
        super(new LauncherModuleInfo("Sentry", Version.of(2, 0, 0), new String[]{"ClientLauncherCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if (config.dsn == null || "YOUR_DSN".equals(config.dsn)) {
            LogHelper.warning("Sentry module disabled. Please configure dsn");
            return;
        }
        registerEvent(this::onInit, ClientEngineInitPhase.class);
        registerEvent(this::onClientInit, ClientProcessInitPhase.class);
        registerEvent(this::beforeStartClient, ClientProcessPreInvokeMainClassEvent.class);
    }

    public void onInit(ClientEngineInitPhase event) {
        initSentry(event.engine);
    }

    public void onClientInit(ClientProcessInitPhase event) {
        initSentry(null);
    }

    public void initSentry(LauncherEngine engine) {
        if (Sentry.isEnabled()) {
            return;
        }
        LogHelper.debug("Initialize Sentry");
        Sentry.init(options -> {
            options.addEventProcessor(new SentryEventProcessor());
            options.setDsn(config.dsn);
            options.setEnvironment(engine == null ? "CLIENT" : "LAUNCHER");
            options.setRelease(Version.getVersion().getVersionString().concat(".").concat(String.valueOf(Launcher.getConfig().buildNumber)));
            if(proguardUuid != null) {
                options.setProguardUuid(proguardUuid);
            }
        }, true);
        currentScopes = Sentry.getCurrentScopes();
        Sentry.configureScope(scope -> {
            BasicProperties.setupBasicProperties((Scope) scope);
            OshiUtils.systemProperties((Scope) scope);
        });
        LogHelper.addExcCallback(Sentry::captureException);
        if (Request.isAvailable()) {
            Request.getRequestService().registerEventHandler(new SentryEventHandler());
        }
        LogHelper.debug("Sentry initialized");
    }

    public void beforeStartClient(ClientProcessPreInvokeMainClassEvent event) {
        Sentry.configureScope(scope -> {
            scope.setUser(makeSentryUser(LauncherEngine.clientParams.playerProfile));
            ClientProfile profile = AuthService.profile;
            scope.setTag("minecraftVersion", profile.getVersion().toString());
            Map<String, String> profileParams = new HashMap<>();
            profileParams.put("MinecraftVersion", profile.getVersion().toString());
            profileParams.put("Name", profile.getTitle());
            profileParams.put("UUID", profile.getUUID().toString());
            scope.setContexts("Profile", profileParams);
        });
    }

    protected static User makeSentryUser(PlayerProfile playerProfile) {
        User sentryUser = new User();
        sentryUser.setUsername(playerProfile.username);
        sentryUser.setId(playerProfile.uuid.toString());
        return sentryUser;
    }
}

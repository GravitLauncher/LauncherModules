package pro.gravit.launchermodules.sentryl;

import io.sentry.IHub;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.LauncherInject;
import pro.gravit.launcher.api.AuthService;
import pro.gravit.launcher.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.client.ClientProcessInitPhase;
import pro.gravit.launcher.client.events.client.ClientProcessPreInvokeMainClassEvent;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.PlayerProfile;
import pro.gravit.launcher.request.Request;
import pro.gravit.launchermodules.sentryl.utils.BasicProperties;
import pro.gravit.launchermodules.sentryl.utils.OshiUtils;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

import java.util.HashMap;
import java.util.Map;

public class SentryModule extends LauncherModule {

    public static Config config = new Config();

    public static IHub currentHub;

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
        currentHub = Sentry.getCurrentHub();
        Sentry.configureScope(scope -> {
            BasicProperties.setupBasicProperties(scope);
            OshiUtils.systemProperties(scope);
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

package pro.gravit.launchermodules.sentryl;

import io.sentry.Scope;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.LauncherEngine;
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
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import java.util.HashMap;
import java.util.Map;

public class SentryModule extends LauncherModule {

    public static Config config = new Config();

    public SentryModule() {
        super(new LauncherModuleInfo("Sentry", Version.of(2,0,0), new String[]{"ClientLauncherCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        if(config.dsn == null || "YOUR_DSN".equals(config.dsn)) {
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
        initSentry(event.clientInstance);
    }

    public void initSentry(LauncherEngine engine) {
        if(Sentry.isEnabled()) {
            return;
        }
        LogHelper.debug("Initialize Sentry");
        Sentry.init(options -> {
            options.setDsn(config.dsn);
            options.setEnvironment(engine.clientInstance ? "CLIENT" : "LAUNCHER");
        });
        Sentry.configureScope(scope -> {
            setupBasicProperties(scope);
        });
        LogHelper.addExcCallback(Sentry::captureException);
        //if (config.captureAll)
        //    LogHelper.addOutput(Sentry::captureMessage, LogHelper.OutputTypes.PLAIN);
        if(Request.isAvailable()) {
            Request.getRequestService().registerEventHandler(new SentryEventHandler());
        }
        LogHelper.debug("Sentry initialized");
    }

    public void beforeStartClient(ClientProcessPreInvokeMainClassEvent event) {
        Sentry.configureScope(scope -> {
            scope.setUser(makeSentryUser(LauncherEngine.clientParams.playerProfile));
            ClientProfile profile = AuthService.profile;
            scope.setTag("minecraftVersion", profile.getVersion().name);
            Map<String, String> profileParams = new HashMap<>();
            profileParams.put("MinecraftVersion", profile.getVersion().name);
            profileParams.put("Name", profile.getTitle());
            profileParams.put("UUID", profile.getUUID().toString());
            scope.setContexts("Profile", profileParams);
        });
    }

    protected static User makeSentryUser(PlayerProfile playerProfile) {
        User sentryUser = new User();
        sentryUser.setUsername(playerProfile.username);
        Map<String, String> params = new HashMap<>();
        params.put("uuid", playerProfile.uuid.toString());
        sentryUser.setData(params);
        return sentryUser;
    }

    private void setupBasicProperties(Scope scope) {
        Map<String, String> os = new HashMap<>();
        os.put("Name", System.getProperties().getProperty("os.name"));
        os.put("Arch", String.valueOf(JVMHelper.ARCH_TYPE));
        scope.setContexts("OS", os);
        scope.setTag("OS_TYPE", System.getProperties().getProperty("os.name"));
        Map<String, String> jvm = new HashMap<>();
        jvm.put("Version", String.valueOf(JVMHelper.JVM_VERSION));
        jvm.put("Bits", String.valueOf(JVMHelper.JVM_BITS));
        jvm.put("runtime_mxbean", String.valueOf(JVMHelper.RUNTIME_MXBEAN.getVmVersion()));

        scope.setContexts("Java", jvm);
        scope.setTag("JVM_VERSION", String.valueOf(JVMHelper.JVM_VERSION));
        Map<String, String> systemProperties = new HashMap<>();
        systemProperties.put("Name", System.getProperties().getProperty("os.name"));
        systemProperties.put("file.encoding", System.getProperties().getProperty("file.encoding"));
        systemProperties.put("java.class.path", System.getProperties().getProperty("java.class.path"));
        systemProperties.put("java.class.version", System.getProperties().getProperty("java.class.version"));
        systemProperties.put("java.endorsed.dirs", System.getProperties().getProperty("java.endorsed.dirs"));
        systemProperties.put("java.ext.dirs", System.getProperties().getProperty("java.ext.dirs"));
        systemProperties.put("java.home", System.getProperties().getProperty("java.home"));
        systemProperties.put("java.io.tmpdir", System.getProperties().getProperty("java.io.tmpdir"));
        systemProperties.put("os.arch", System.getProperties().getProperty("os.arch"));
        systemProperties.put("sun.arch.data.model", System.getProperties().getProperty("sun.arch.data.model"));
        systemProperties.put("sun.boot.class.path", System.getProperties().getProperty("sun.boot.class.path"));
        systemProperties.put("sun.cpu.isalist", System.getProperties().getProperty("sun.cpu.isalist"));
        systemProperties.put("sun.jnu.encoding", System.getProperties().getProperty("sun.jnu.encoding"));
        systemProperties.put("user.language", System.getProperties().getProperty("user.language"));
        systemProperties.put("user.timezone", System.getProperties().getProperty("user.timezone"));
        scope.setContexts("SystemProperties", systemProperties);
    }
}

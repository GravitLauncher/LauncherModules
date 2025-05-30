package pro.gravit.launchermodules.discordgame;

import pro.gravit.launcher.base.modules.LauncherModuleInfoBuilder;
import pro.gravit.launcher.runtime.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.client.events.ClientUnlockConsoleEvent;
import pro.gravit.launcher.runtime.client.events.ClientProcessBuilderParamsWrittedEvent;
import pro.gravit.launcher.client.events.ClientProcessLaunchEvent;
import pro.gravit.launcher.base.modules.LauncherInitContext;
import pro.gravit.launcher.base.modules.LauncherModule;
import pro.gravit.launcher.base.request.Request;
import pro.gravit.launchermodules.discordgame.commands.DiscordCommand;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.LogHelper;

public class ClientModule extends LauncherModule {
    public static final Version version = new Version(1, 1, 0, 1, Version.Type.LTS);
    private static final Object lock = new Object();
    public static Config config;
    public static ScopeConfig loginScopeConfig;
    public static ScopeConfig authorizedScopeConfig;
    public static ScopeConfig clientScopeConfig;
    private static volatile boolean isClosed = false;

    public ClientModule() {
        super(new LauncherModuleInfoBuilder().setName("DiscordGame").setVersion(version).setDependencies(new String[]{"ClientLauncherCore"}).createLauncherModuleInfo());
    }

    /**
     * @param flag Set closed to true?
     * @return Current closed.
     */
    public static boolean isClosed(boolean flag) {
        boolean ret;
        synchronized (lock) {
            ret = isClosed;
            if (flag) isClosed = true;
            lock.notify();
        }
        return ret;
    }


    @Override
    public void init(LauncherInitContext initContext) {
        config = new Config();
        loginScopeConfig = new ScopeConfig(config.scopes.get("login"));
        authorizedScopeConfig = new ScopeConfig(config.scopes.get("authorized"));
        clientScopeConfig = new ScopeConfig(config.scopes.get("client"));
        registerEvent(this::clientInit, ClientProcessLaunchEvent.class);
        registerEvent(this::launcherInit, ClientEngineInitPhase.class);
        registerEvent(this::exitHandler, ClientExitPhase.class);
        registerEvent(this::exitByStartClient, ClientProcessBuilderParamsWrittedEvent.class);
    }

    private void clientInit(ClientProcessLaunchEvent phase) {
        DiscordBridge.activityService.updateClientStage(phase.params);
        try {
            DiscordBridge.init(config.appId, true);
            RequestEventWatcher.INSTANCE = new RequestEventWatcher(true);
            Request.getRequestService().registerEventHandler(RequestEventWatcher.INSTANCE);
        } catch (Throwable e) {
            LogHelper.error(e);
        }
    }

    private void unlock(ClientUnlockConsoleEvent event) {
        event.handler.registerCommand("discord", new DiscordCommand());
    }

    private void launcherInit(ClientEngineInitPhase phase) {
        DiscordBridge.activityService.updateLoginStage();
        try {
            DiscordBridge.init(config.appId, false);
            RequestEventWatcher.INSTANCE = new RequestEventWatcher(false);
            Request.getRequestService().registerEventHandler(RequestEventWatcher.INSTANCE);
        } catch (Throwable e) {
            LogHelper.error(e);
        }
    }

    private void exitHandler(ClientExitPhase phase) {
        if (isClosed(true)) return;
        if (RequestEventWatcher.INSTANCE != null)
            Request.getRequestService().unregisterEventHandler(RequestEventWatcher.INSTANCE);
        DiscordBridge.close();
    }

    private void exitByStartClient(ClientProcessBuilderParamsWrittedEvent event) {

    }

}

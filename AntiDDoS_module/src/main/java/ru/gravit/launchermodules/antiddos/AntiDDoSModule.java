package ru.gravit.launchermodules.antiddos;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;
import ru.gravit.utils.Version;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class AntiDDoSModule implements Module {
    public static Version version = new Version(1,0,0,0,Version.Type.BETA);
    public static Path configfile = Paths.get("anti-ddos.json");
    public static class Config
    {
        public boolean disableSocketFatalErrors = false;
        public int maxFails = 3;
        public boolean printBannedMessage = true;
        public boolean printTryConnectionMessage = true;
        public ArrayList<String> whitelist;
    }
    public static Config config;
    public static BanIPProtector banIPProtector;
    @Override
    public String getName() {
        return "Gravit Anti-DDoS";
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void init(ModuleContext context1) {
        LaunchServerModuleContext context = (LaunchServerModuleContext) context1;
        LogHelper.debug("Init anti-DDoS");
        if(IOHelper.exists(configfile))
        {
            try(Reader reader = IOHelper.newReader(configfile)) {
                config = LaunchServer.gson.fromJson(reader,Config.class);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        else
        {
            LogHelper.debug("Create new anti-ddos config file");
            try(Writer writer = IOHelper.newWriter(configfile))
            {
                config = new Config();
                LaunchServer.gson.toJson(config, writer);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        banIPProtector = new BanIPProtector();
        banIPProtector.whitelist.addAll(config.whitelist);
        context.launchServer.socketHookManager.registerFatalErrorHook(banIPProtector);
    }

    @Override
    public void postInit(ModuleContext context1) {
        LaunchServerModuleContext context = (LaunchServerModuleContext) context1;
        context.launchServer.serverSocketHandler.setListener(banIPProtector);
    }

    @Override
    public void preInit(ModuleContext context) {

    }

    @Override
    public void close() throws Exception {

    }
    public static void main(String[] args)
    {

    }
}

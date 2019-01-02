package ru.gravit.launchermodules.antiddos;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.Reconfigurable;
import ru.gravit.launchserver.Reloadable;
import ru.gravit.launchserver.modules.LaunchServerModuleContext;
import ru.gravit.utils.Version;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;

public class AntiDDoSModule implements Module, Reloadable, Reconfigurable {
    public static final Version version = new Version(1, 0, 1, 3, Version.Type.BETA);

    public static class Config
    {
        public boolean disableSocketFatalErrors = false;
        public int maxFails = 3;
        public boolean printBannedMessage = true;
        public boolean printTryConnectionMessage = true;
        public ArrayList<String> whitelist;
    }
    
    public Path configfile;
    public Config config;
    public BanIPProtector banIPProtector;
	public LaunchServer srv;
	
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
        srv = context.launchServer;
        configfile = context.launchServer.dir.resolve("anti-ddos.json");
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
        banIPProtector = new BanIPProtector( this);
        banIPProtector.whitelist.addAll(config.whitelist);
        context.launchServer.socketHookManager.registerFatalErrorHook(banIPProtector);
        context.launchServer.reloadManager.registerReloadable("antiddos",this);
        context.launchServer.reconfigurableManager.registerReconfigurable("antiddos",this);
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
    public void reload() throws Exception {
        try(Reader reader = IOHelper.newReader(configfile)) {
            config = LaunchServer.gson.fromJson(reader,Config.class);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public void reconfig(String action, String[] args) {
        if(action.equals("clear"))
        {
            banIPProtector.banlist.clear();
            LogHelper.info("IP BanList clean");
        }
        else if(action.equals("remove"))
        {
            banIPProtector.banlist.remove(args[0]);
            LogHelper.info("IP %s unbanned",args[0]);
        }
    }

    @Override
    public void printConfigHelp() {
        LogHelper.info("clean [none] - clean banlist");
        LogHelper.info("remove [ip] - remove ip from banlist");
    }
    
    @Override
    public void close() throws Exception {

    }
    
    public static void main(String[] args)
    {
    	System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }
}

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

public class AntiDDoSModule implements Module, Reloadable, Reconfigurable {
    public static final Version version = new Version(1, 0, 1, 3, Version.Type.BETA);

    public static class Config {
        public boolean disableSocketFatalErrors = false;
        public int maxFails = 3;
        public boolean printBannedMessage = true;
        public boolean printTryConnectionMessage = true;
    }

    public Path configFile;
    public Path bansFile;
    public Path whiteListFile;
    public Path configDir;
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
        configDir = context.modulesConfigManager.getModuleConfigDir(getName());
        bansFile = configDir.resolve("banList.lst");
        whiteListFile = configDir.resolve("whiteList.lst");
        configFile = context.modulesConfigManager.getModuleConfig(getName());
        if (IOHelper.exists(configFile)) {
            try (Reader reader = IOHelper.newReader(configFile)) {
                config = LaunchServer.gson.fromJson(reader, Config.class);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        } else {
            LogHelper.debug("Create new anti-ddos config file");
            try (Writer writer = IOHelper.newWriter(configFile)) {
                config = new Config();
                LaunchServer.gson.toJson(config, writer);
            } catch (IOException e) {
                LogHelper.error(e);
            }
        }
        banIPProtector = new BanIPProtector(this);
        banIPProtector.deserialize(bansFile, whiteListFile);
        context.launchServer.socketHookManager.registerFatalErrorHook(banIPProtector);
        context.launchServer.reloadManager.registerReloadable("antiddos", this);
        context.launchServer.reconfigurableManager.registerReconfigurable("antiddos", this);
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
    public void reload() {
        try (Reader reader = IOHelper.newReader(configFile)) {
            config = LaunchServer.gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }

    @Override
    public void reconfig(String action, String[] args) {
        switch (action) {
        case "clean": {
        	 banIPProtector.banList.clear();
        }
        case "remove": {
        	if (args.length < 1) {
        		LogHelper.error("Invalid arguments.");
        		return;
        	}
        	banIPProtector.banList.remove(args[0]);
            LogHelper.info("IP %s unbanned", args[0]);
        }
        case "saveBans": {
        	banIPProtector.serialize(bansFile, whiteListFile);
        }
        case "readBans": {
        	banIPProtector.deserialize(bansFile, whiteListFile);
        }
        default: {
        	LogHelper.error("Invalid command.");
        }
        }
    }

    @Override
    public void printConfigHelp() {
        LogHelper.info("clean [none] - clean banlist");
        LogHelper.info("remove [ip] - remove ip from banlist");
        LogHelper.info("saveBans [none] - save banList");
        LogHelper.info("readBans [none] - read banList");
    }

    @Override
    public void close() {
    	banIPProtector.serialize(bansFile, whiteListFile);
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }
}

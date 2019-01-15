package ru.gravit.launchermodules.antiddos;

import ru.gravit.launchserver.manangers.hook.SocketHookManager;
import ru.gravit.launchserver.socket.ServerSocketHandler;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BanIPProtector implements SocketHookManager.SocketFatalErrorHook, ServerSocketHandler.Listener {
    private AntiDDoSModule mod;

    public BanIPProtector(AntiDDoSModule mod) {
        this.mod = mod;
    }

    @Override
    public boolean fatalErrorHook(Socket socket, Exception ex) {
        String ip = IOHelper.getIP(socket.getRemoteSocketAddress());
        if (whitelist.contains(ip)) return !mod.config.disableSocketFatalErrors;
        if (!banlist.containsKey(ip)) banlist.put(ip, new Entry(1));
        else {
            Entry e = banlist.get(ip);
            e.fails++;
            if (mod.config.printBannedMessage && e.fails >= mod.config.maxFails) {
                LogHelper.warning("IP %s banned", ip);
            }
        }
        return !mod.config.disableSocketFatalErrors;
    }

    @Override
    public boolean onConnect(InetAddress address) {
        String ip = address.getHostAddress();
        if (!banlist.containsKey(ip)) banlist.put(ip, new Entry(0));
        else {
            Entry e = banlist.get(ip);
            if (e.fails >= mod.config.maxFails) {
                e.tryconnection++;
                if (mod.config.printTryConnectionMessage)
                    LogHelper.info("IP %s try connection #%d", ip, e.tryconnection);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDisconnect(Exception e) {

    }

    @Override
    public boolean onHandshake(long session, int type) {
        return false;
    }

    public class Entry {
        public Entry(int fails) {
            this.fails = fails;
            this.tryconnection = 0;
        }

        int fails;
        int tryconnection;
    }

    public Map<String, Entry> banlist = new ConcurrentHashMap<>();
    public Set<String> whitelist = new HashSet<>();
}

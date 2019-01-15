package ru.gravit.launchermodules.antiddos;

import ru.gravit.launchserver.manangers.hook.SocketHookManager;
import ru.gravit.launchserver.socket.ServerSocketHandler;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
        if (whiteList.contains(ip)) return !mod.config.disableSocketFatalErrors;
        if (!banList.containsKey(ip)) banList.put(ip, new Entry(1));
        else {
            Entry e = banList.get(ip);
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
        if (!banList.containsKey(ip)) banList.put(ip, new Entry(0));
        else {
            Entry e = banList.get(ip);
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

    public static final class Entry {
        public Entry(int fails) {
            this.fails = fails;
            this.tryconnection = 0;
        }

        public int fails;
        public int tryconnection;
    }

    public void serialize(Path banList, Path whiteList) {
    	try (PrintWriter out = new PrintWriter(IOHelper.newWriter(banList))) {
    		this.banList.forEach((k, v) -> {
    			if (v.fails >= mod.config.maxFails) out.println(k + " " + Integer.toString(v.fails));
    		});
    	} catch (IOException ioexc) {
    		LogHelper.error(ioexc);
    	}
    	try (PrintWriter out = new PrintWriter(IOHelper.newWriter(whiteList))) {
    		this.whiteList.forEach(out::println);
    	} catch (IOException ioexc) {
    		LogHelper.error(ioexc);
    	}
    }
    
    public void deserialize(Path banList, Path whiteList) {
    	this.whiteList.clear();
    	this.banList.clear();
    	if (Files.exists(banList))
    		try (BufferedReader reader = IOHelper.newReader(banList)) {
    			String line = reader.readLine();
				while (line != null) {
					if (!line.isEmpty()) {
						String[] parts = line.split(" ");
						if (parts.length > 1)
							this.banList.put(parts[0], new Entry(Integer.parseInt(parts[1])));
					}
	                line = reader.readLine();
	            }
    		} catch (IOException ioexc) {
        		LogHelper.error(ioexc);
        	}
    	if (Files.exists(whiteList))
			try (BufferedReader reader = IOHelper.newReader(whiteList)) {
				String line = reader.readLine();
				while (line != null) {
					if (!line.isEmpty()) this.whiteList.add(line);
	                line = reader.readLine();
	            }
			} catch (IOException ioexc) {
				LogHelper.error(ioexc);
			}
		else {
			this.whiteList.add("localhost");
    		this.whiteList.add("127.0.0.1");
    	} 
    }
    public Map<String, Entry> banList = new ConcurrentHashMap<>();
    public Set<String> whiteList = Collections.newSetFromMap(new ConcurrentHashMap<>());
}

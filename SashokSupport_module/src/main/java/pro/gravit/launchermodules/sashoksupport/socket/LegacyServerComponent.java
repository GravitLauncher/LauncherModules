package pro.gravit.launchermodules.sashoksupport.socket;

import pro.gravit.launchermodules.sashoksupport.command.LogConnectionsCommand;
import pro.gravit.launchermodules.sashoksupport.command.RebindCommand;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.components.Component;
import pro.gravit.utils.helper.CommonHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.spec.InvalidKeySpecException;

public class LegacyServerComponent extends Component {
    public static boolean registerCommands = false;
    public String bindAddress;
    public int port;
    public int threadCoreCount;
    public int threadCount;
    public String launcherFile;
    public String launcherEXEFile;
    public String oldRSAPrivateKey;
    public transient EFileInfo launcher;
    public transient EFileInfo launcherEXE;
    public transient ServerSocketHandler handler;

    public SocketAddress getSocketAddress() {
        return new InetSocketAddress(bindAddress, port);
    }

    @Override
    public void init(LaunchServer launchServer) {
        Path key = Paths.get(oldRSAPrivateKey);
        try {
            launcher = new EFileInfo(launcherFile, key);
            launcherEXE = new EFileInfo(launcherEXEFile, key);
        } catch (IOException | InvalidKeySpecException e) {
            throw new RuntimeException("Invalid launcher file", e);
        }
        handler = new ServerSocketHandler(this);
        CommonHelper.newThread("Legacy Sashok Server", true, handler);
        if (!registerCommands) {
            launchServer.commandHandler.registerCommand("logConnections", new LogConnectionsCommand(launchServer));
            launchServer.commandHandler.registerCommand("rebind", new RebindCommand(launchServer));
            registerCommands = true;
        }
    }
}

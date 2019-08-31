package pro.gravit.launchermodules.sashoksupport.socket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

import pro.gravit.launchermodules.sashoksupport.command.LogConnectionsCommand;
import pro.gravit.launchermodules.sashoksupport.command.RebindCommand;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.components.Component;
import pro.gravit.utils.helper.CommonHelper;

public class LegacyServerComponent extends Component {
    public static boolean registerCommands = false;
    public String bindAddress;
    public int port;
    public int threadCoreCount;
    public int threadCount;
    public String launcherFile;
    public String launcherEXEFile;
    public transient Path launcher;
    public transient Path launcherEXE;
    public transient LaunchServer launchServer;
    public transient ServerSocketHandler handler;
    public SocketAddress getSocketAddress() {
        return new InetSocketAddress(bindAddress, port);
    }
    @Override
    public void preInit(LaunchServer launchServer) {
        this.launchServer = launchServer;
    }

    @Override
    public void init(LaunchServer launchServer) {

    }

    @Override
    public void postInit(LaunchServer launchServer) {
        launcher = Paths.get(launcherFile);
        launcherEXE = Paths.get(launcherEXEFile);
        handler = new ServerSocketHandler(launchServer, this);
        CommonHelper.newThread("Legacy Sashok Server", true, handler);
        if(!registerCommands)
        {
            launchServer.commandHandler.registerCommand("logConnections", new LogConnectionsCommand(launchServer));
            launchServer.commandHandler.registerCommand("rebind", new RebindCommand(launchServer));
            registerCommands = true;
        }
    }
}

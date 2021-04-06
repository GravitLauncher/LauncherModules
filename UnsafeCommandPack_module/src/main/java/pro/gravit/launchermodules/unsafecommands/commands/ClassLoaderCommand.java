package pro.gravit.launchermodules.unsafecommands.commands;

import pro.gravit.launchermodules.unsafecommands.UnsafeURLClassLoader;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;

public class ClassLoaderCommand extends Command {
    public ClassLoaderCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[new/addURL/manualDefine] [name] [args]";
    }

    @Override
    public String getUsageDescription() {
        return null;
    }

    @Override
    public void invoke(String... args) throws Exception {
        verifyArgs(args, 3);
        String cmd = args[0];
        String name = args[1];
        switch (cmd) {
            case "new": {
                UnsafeURLClassLoader.classLoaderMap.put(name, new UnsafeURLClassLoader(getURL(args[2])));
                break;
            }
            case "addURL": {
                UnsafeURLClassLoader cl = UnsafeURLClassLoader.classLoaderMap.get(name);
                if (cl == null)
                    throw new NullPointerException(String.format("UnsafeURLClassLoader %s not found", name));
                for (URL u : getURL(args[2])) {
                    cl.addURL(u);
                }
            }
            case "manualDefine": {
                UnsafeURLClassLoader cl = UnsafeURLClassLoader.classLoaderMap.get(name);
                if (cl == null)
                    throw new NullPointerException(String.format("UnsafeURLClassLoader %s not found", name));
                byte[] clazzBytes = IOHelper.read(Paths.get(args[2]));
                cl.rawDefineClass(args.length > 3 ? args[3] : null, clazzBytes, args.length > 4 ? Integer.parseInt(args[4]) : 0, args.length > 5 ? Integer.parseInt(args[5]) : clazzBytes.length);
            }
        }
    }

    public URL[] getURL(String s) {
        String[] splits = s.split(";");
        return Arrays.stream(splits).map((e) -> {
            try {
                return new URL(e);
            } catch (MalformedURLException ex) {
                LogHelper.error(ex);
                return null;
            }
        }).toArray(URL[]::new);
    }
}

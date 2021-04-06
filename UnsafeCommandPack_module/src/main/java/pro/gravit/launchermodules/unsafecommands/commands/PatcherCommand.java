package pro.gravit.launchermodules.unsafecommands.commands;

import pro.gravit.launchermodules.unsafecommands.patcher.UnsafePatcher;
import pro.gravit.launchermodules.unsafecommands.patcher.impl.*;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PatcherCommand extends Command {
    public static Map<String, UnsafePatcher> patchers = new HashMap<>();

    public PatcherCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[patcher name or class] [path] [test mode(true/false)] (other args)";
    }

    @Override
    public String getUsageDescription() {
        return "";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void invoke(String... args) throws Exception {
        if (patchers.isEmpty()) {
            patchers.put("findSystem", new FindSystemPatcher());
            patchers.put("findRemote", new FindRemotePatcher());
            patchers.put("findSun", new FindSunPatcher());
            patchers.put("findPacketHack", new FindPacketHackPatcher());
            patchers.put("findDefineClass", new FindDefineClassPatcher());
            patchers.put("findReflect", new FindReflectPatcher());
        }
        verifyArgs(args, 3);
        String name = args[0];
        Path target = Paths.get(args[1]);
        boolean testMode = Boolean.parseBoolean(args[2]);
        UnsafePatcher patcher = patchers.get(name);
        if (patcher == null) {
            Class<? extends UnsafePatcher> clazz = (Class<? extends UnsafePatcher>) Class.forName(name);
            try {
                String[] real_args = Arrays.copyOfRange(args, 3, args.length);
                if (real_args.length > 0)
                    patcher = (UnsafePatcher) MethodHandles.publicLookup().findConstructor(clazz, MethodType.methodType(void.class, String[].class)).asFixedArity().invoke(real_args);
                else
                    patcher = (UnsafePatcher) MethodHandles.publicLookup().findConstructor(clazz, MethodType.methodType(void.class)).invoke();
            } catch (Throwable e) {
                LogHelper.dev(LogHelper.toString(e));
                try {
                    patcher = (UnsafePatcher) MethodHandles.publicLookup().findConstructor(clazz, MethodType.methodType(void.class)).invokeWithArguments();
                } catch (Throwable t) {
                    throw (InstantiationException) new InstantiationException().initCause(t);
                }
            }
        }
        if (!IOHelper.exists(target))
            throw new IllegalStateException("Target path not exist");
        Path tempFile = server.dir.resolve("build").resolve("patcher.tmp.jar");
        if (IOHelper.isFile(target)) {
            patcher.processFile(target, tempFile, testMode);
        } else if (IOHelper.isDir(target)) {
            patcher.processDir(target, tempFile, testMode);
        }
    }
}

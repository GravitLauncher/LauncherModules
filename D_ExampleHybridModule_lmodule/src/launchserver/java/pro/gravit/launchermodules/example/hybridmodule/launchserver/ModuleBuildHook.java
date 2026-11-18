package pro.gravit.launchermodules.example.hybridmodule.launchserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.BuildContext;
import pro.gravit.launchserver.binary.tasks.MainBuildTask.ASMTransformer;
import pro.gravit.launchserver.launchermodules.LauncherModuleLoader.ModuleEntity;

import java.io.IOException;

public final class ModuleBuildHook {
    private static final Logger logger = LogManager.getLogger(ModuleBuildHook.class);
    private static final String SERVER_PACKAGE = ModuleBuildHook.class.getPackageName().replace('.', '/') + "/";
    private static final String EXAMPLE_CLASS = "pro.gravit.launchermodules.example.hybridmoule.ExampleClass";

    private ModuleBuildHook() {
    }

    public static void preBuild(LaunchServer server, BuildContext context, ModuleEntity module) {
        logger.info("Preparing launcher module {}", module.moduleMainClass);
        ASMTransformer transformer = ModuleBuildHook::transformExampleClass;
        context.transformers.add(transformer);
    }

    private static void transformExampleClass(ClassNode classNode, String className, BuildContext context) {
        if (!EXAMPLE_CLASS.equals(className)) {
            return;
        }

        for (var method : classNode.methods) {
            if (!method.name.equals("message") || !method.desc.equals("()Ljava/lang/String;")) {
                continue;
            }
            for (var instruction : method.instructions) {
                if (instruction instanceof LdcInsnNode constant && "Original example message".equals(constant.cst)) {
                    constant.cst = "Example message transformed by LaunchServer";
                }
            }
        }
    }

    public static boolean postBuild(LaunchServer server, BuildContext context, ModuleEntity module) throws IOException {
        context.pushJarFile(module.path, entry -> entry.getName().startsWith(SERVER_PACKAGE), className -> true);
        return false;
    }
}

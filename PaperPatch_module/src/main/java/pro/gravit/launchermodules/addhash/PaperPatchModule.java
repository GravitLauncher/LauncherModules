package pro.gravit.launchermodules.addhash;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import pro.gravit.launcher.modules.Module;
import pro.gravit.launcher.modules.ModuleContext;
import pro.gravit.launcher.server.ServerAgent;
import pro.gravit.utils.Version;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;

public final class PaperPatchModule implements Module {

    private static final Version VERSION = new Version(1, 0, 0);

    @Override
    public String getName() {
        return "PaerPatch";
    }

    @Override
    public Version getVersion() {
        return VERSION;
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public void init(ModuleContext context) {

    }

    @Override
    public void postInit(ModuleContext context) {
    }

    @Override
    public void preInit(ModuleContext context) {
        Instrumentation instrumentation = ServerAgent.inst;
        if (instrumentation == null) {
            throw new IllegalStateException("PaperPatch requires instrumentation to work!");
        }
        ClassFileTransformer transformer = new ClassFileTransformer() {

            private Pattern pattern = Pattern.compile("net/minecraft/server/[^/]+/MinecraftServer");

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                Pattern pattern = this.pattern;
                if (pattern != null && pattern.matcher(className).matches()) {
                    this.pattern = null;
                    ClassReader cr = new ClassReader(classfileBuffer);
                    ClassWriter cw = new ClassWriter(cr, 0);
                    cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            MethodVisitor original = super.visitMethod(access, name, descriptor, signature, exceptions);
                            if ("main".equals(name) && "(Ljoptsimple/OptionSet;)V".equals(descriptor)) {
                                return new MethodVisitor(Opcodes.ASM5, original) {

                                    @Override
                                    public void visitTypeInsn(int opcode, String type) {
                                        if ("com/destroystokyo/paper/profile/PaperMinecraftSessionService".equals(type)) {
                                            type = "com/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService";
                                        }
                                        super.visitTypeInsn(opcode, type);
                                    }

                                    @Override
                                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                                        if ("<init>".equals(name) && "com/destroystokyo/paper/profile/PaperMinecraftSessionService".equals(owner)) {
                                            owner = "com/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService";
                                        }
                                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                    }
                                };
                            }
                            return original;
                        }
                    }, 0);
                    return cw.toByteArray();
                }
                return classfileBuffer;
            };
        };
        instrumentation.addTransformer(transformer, false);
    }

    @Override
    public void close() throws Exception {
    }
}

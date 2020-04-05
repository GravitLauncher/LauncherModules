package pro.gravit.launchermodules.unsafecommands.patcher.impl;

import org.objectweb.asm.*;
import pro.gravit.launchermodules.unsafecommands.patcher.ClassTransformerPatcher;
import pro.gravit.utils.helper.LogHelper;

import java.util.ArrayList;
import java.util.List;

public class FindSystemPatcher extends ClassTransformerPatcher {
    public static List<String> noTriggeredMethods = new ArrayList<>();
    public static List<String> noTriggeredMethodsCL = new ArrayList<>();

    static {
        noTriggeredMethods.add("currentTimeMillis");
        noTriggeredMethods.add("getProperty");
        noTriggeredMethods.add("arraycopy");
        noTriggeredMethods.add("identityHashCode");
        noTriggeredMethods.add("nanoTime");

        noTriggeredMethodsCL.add("getSystemResource");
        noTriggeredMethodsCL.add("getSystemResourceAsStream");
        noTriggeredMethodsCL.add("getSystemResources");
    }

    @Override
    public ClassVisitor getVisitor(ClassReader reader, ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM7) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM7) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && (("java/lang/System".equals(owner) && !noTriggeredMethods.contains(name)) || ("java/lang/ClassLoader".equals(owner) && !noTriggeredMethodsCL.contains(name)))) {
                            LogHelper.info("Class %s method %s call %s.%s(%s)", reader.getClassName(), methodName, owner, name, descriptor);
                        }
                        if ("defineClass".equals(name)) {
                            LogHelper.info("Class %s method %s call %s.%s(%s)", reader.getClassName(), methodName, owner, name, descriptor);
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                };
            }
        };
    }
}

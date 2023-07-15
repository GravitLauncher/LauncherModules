package pro.gravit.launchermodules.unsafecommands.patcher.impl;

import org.objectweb.asm.*;
import pro.gravit.launchermodules.unsafecommands.patcher.ClassTransformerPatcher;
import pro.gravit.utils.helper.LogHelper;

import java.util.ArrayList;
import java.util.List;

public class FindSunPatcher extends ClassTransformerPatcher {
    public static List<String> noTriggeredMethods = new ArrayList<>();

    @Override
    public ClassVisitor getVisitor(ClassReader reader, ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM7) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if (value instanceof String string && isUnsafe(string)) {
                    LogHelper.info("Class %s field %s: %s", reader.getClassName(), name, value);
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM7) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (owner != null && isUnsafe(owner)) {
                            LogHelper.info("Class %s method %s call %s.%s(%s)", reader.getClassName(), methodName, owner, name, descriptor);
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String string && isUnsafe(string)) {
                            LogHelper.info("Class %s method %s LDC %s", reader.getClassName(), methodName, value);
                        }
                        super.visitLdcInsn(value);
                    }
                };
            }
        };
    }

    public boolean isUnsafe(String name) {
        if ((name.startsWith("com/sun/") || name.startsWith("com.sun.")) && !(name.startsWith("com/sun/jna") || name.startsWith("com.sun.jna")))
            return true;
        if (name.startsWith("jdk/") || name.startsWith("jdk.")) return true;
        return name.startsWith("sun/") || name.startsWith("sun.");
    }
}

package pro.gravit.launchermodules.unsafecommands.patcher.impl;

import org.objectweb.asm.*;
import pro.gravit.launchermodules.unsafecommands.patcher.ClassTransformerPatcher;
import pro.gravit.utils.helper.LogHelper;

public class FindDefineClassPatcher extends ClassTransformerPatcher {
    @Override
    public ClassVisitor getVisitor(ClassReader reader, ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM7) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM7) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && "java/lang/ClassLoader".equals(owner) && "defineClass".equals(name)) {
                            LogHelper.info("Class %s method %s call %s.%s(%s)", reader.getClassName(), methodName, owner, name, descriptor);
                        } else if (opcode == Opcodes.INVOKEVIRTUAL && "java/security/SecureClassLoader".equals(owner) && "defineClass".equals(name)) {
                            LogHelper.info("Class %s method %s call %s.%s(%s)", reader.getClassName(), methodName, owner, name, descriptor);
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String && ((String) value).contains("defineClass")) {
                            // may be it is reflected call!
                            LogHelper.info("Class %s method %s LDC %s", reader.getClassName(), methodName, value);
                        }
                        super.visitLdcInsn(value);
                    }
                };
            }
        };
    }
}

package pro.gravit.launchermodules.unsafecommands.patcher.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.objectweb.asm.*;
import pro.gravit.launchermodules.unsafecommands.patcher.ClassTransformerPatcher;
import pro.gravit.utils.helper.LogHelper;

public class FindDefineClassPatcher extends ClassTransformerPatcher {

    private static final Logger logger =
            LoggerFactory.getLogger(FindDefineClassPatcher.class);

    @Override
    public ClassVisitor getVisitor(ClassReader reader, ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM7) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM7) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKEVIRTUAL && "java/lang/ClassLoader".equals(owner) && "defineClass".equals(name)) {
                            logger.info("Class {} method {} call {}.{}({})", reader.getClassName(), methodName, owner, name, descriptor);
                        } else if (opcode == Opcodes.INVOKEVIRTUAL && "java/security/SecureClassLoader".equals(owner) && "defineClass".equals(name)) {
                            logger.info("Class {} method {} call {}.{}({})", reader.getClassName(), methodName, owner, name, descriptor);
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String string && string.contains("defineClass")) {
                            // may be it is reflected call!
                            logger.info("Class {} method {} LDC {}", reader.getClassName(), methodName, value);
                        }
                        super.visitLdcInsn(value);
                    }
                };
            }
        };
    }
}
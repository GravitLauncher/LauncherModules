package pro.gravit.launchermodules.unsafecommands.patcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.*;

public class StaticReplacerPatcher extends ClassTransformerPatcher {
    public final String targetOwnerClass;
    public final String targetOwnerMethod;
    public final String replaceOwnerClass;
    public final String replaceOwnerMethod;
    private transient final Logger logger = LogManager.getLogger();

    public StaticReplacerPatcher(String targetOwnerClass, String targetOwnerMethod, String replaceOwnerClass, String replaceOwnerMethod) {
        this.targetOwnerClass = targetOwnerClass;
        this.targetOwnerMethod = targetOwnerMethod;
        this.replaceOwnerClass = replaceOwnerClass;
        this.replaceOwnerMethod = replaceOwnerMethod;
    }

    public StaticReplacerPatcher(String[] args) {
        if (args.length < 4) throw new IllegalArgumentException("Patcher need 4 args");
        targetOwnerClass = toInternalClassFormat(args[0]);
        targetOwnerMethod = toInternalClassFormat(args[1]);
        replaceOwnerClass = toInternalClassFormat(args[2]);
        replaceOwnerMethod = toInternalClassFormat(args[3]);
        logger.info("Create patcher {}.{} replaced to {}.{}", targetOwnerClass, targetOwnerMethod, replaceOwnerClass, replaceOwnerMethod);
    }

    public StaticReplacerPatcher() {
        throw new NullPointerException("Patcher need args!");
    }

    private String toInternalClassFormat(String str) {
        return str.replaceAll("\\.", "/");
    }

    @Override
    public ClassVisitor getVisitor(ClassReader reader, ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM8, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM8, super.visitMethod(access, methodName, descriptor, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC && owner.equals(targetOwnerClass) && name.equals(targetOwnerMethod)) {
                            super.visitMethodInsn(opcode, replaceOwnerClass, replaceOwnerMethod, descriptor, isInterface);
                            logger.info("Class {} method {} call {}.{}({})", reader.getClassName(), methodName, owner, name, descriptor);
                        } else {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    }
                };
            }
        };
    }
}

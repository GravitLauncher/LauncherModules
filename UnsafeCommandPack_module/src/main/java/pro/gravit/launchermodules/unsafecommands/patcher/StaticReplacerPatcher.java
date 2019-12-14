package pro.gravit.launchermodules.unsafecommands.patcher;

import org.objectweb.asm.*;
import pro.gravit.utils.helper.LogHelper;

public class StaticReplacerPatcher extends ClassTransformerPatcher {
    public final String targetOwnerClass;
    public final String targetOwnerMethod;
    public final String replaceOwnerClass;
    public final String replaceOwnerMethod;

    public StaticReplacerPatcher(String targetOwnerClass, String targetOwnerMethod, String replaceOwnerClass, String replaceOwnerMethod) {
        this.targetOwnerClass = targetOwnerClass;
        this.targetOwnerMethod = targetOwnerMethod;
        this.replaceOwnerClass = replaceOwnerClass;
        this.replaceOwnerMethod = replaceOwnerMethod;
    }

    public StaticReplacerPatcher(String[] args) {
        if(args.length < 4) throw new IllegalArgumentException("Patcher need 4 args");
        targetOwnerClass = args[0];
        targetOwnerMethod = args[1];
        replaceOwnerClass = args[2];
        replaceOwnerMethod = args[3];
        LogHelper.info("Create patcher %s.%s replaced to %s.%s", targetOwnerClass, targetOwnerMethod, replaceOwnerClass, replaceOwnerMethod);
    }

    public StaticReplacerPatcher() {
        throw new NullPointerException("Patcher need args!");
    }

    @Override
    public ClassVisitor getVisitor(ClassReader reader, ClassWriter cw) {
        return new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if(opcode == Opcodes.INVOKESTATIC && owner.equals(targetOwnerClass) && name.equals(targetOwnerMethod))
                        {
                            super.visitMethodInsn(opcode, replaceOwnerClass, replaceOwnerMethod, descriptor, isInterface);
                            LogHelper.debug("Class %s method %s call %s.%s(%s)", reader.getClassName(), methodName, owner, name, descriptor);
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                };
            }
        };
    }
}

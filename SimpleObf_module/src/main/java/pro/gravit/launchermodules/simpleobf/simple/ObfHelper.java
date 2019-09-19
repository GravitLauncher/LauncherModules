package pro.gravit.launchermodules.simpleobf.simple;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Base64;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import pro.gravit.launchserver.asm.NodeUtils;

public class ObfHelper {
	public static final MethodNode INDY_SPEC;
	public static final MethodNode INDY_STAT;
	public static final MethodNode INDY_VIRT;
	public static final MethodNode CHECK_CLAZZ;

	static {
		final ClassNode node = NodeUtils.forClass(ObfHelper.class, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);
		MethodNode iSt = null, iVr = null, iSp = null, iCh = null;
		for (final MethodNode m : node.methods) {
			if (!m.name.startsWith("bootstrap"))
				continue;
			switch (m.name) {
			case "bootstrapStatic":
				iSt = m;
				break;
			case "bootstrapVirtualInterface":
				iVr = m;
				break;
			case "bootstrapSpecial":
				iSp = m;
				break;
			case "bootstrapСheckCerts":
				iCh = m;
				break;
			default:
				break;
			}
		}
		INDY_STAT = iSt;
		INDY_VIRT = iVr;
		INDY_SPEC = iSp;
		CHECK_CLAZZ = iCh;
	}

	public static void accept(final ClassVisitor classVisitor, final MethodNode n, final String newName) {
		final MethodVisitor methodVisitor = classVisitor.visitMethod(n.access, newName, n.desc, n.signature, null);
		if (methodVisitor != null)
			n.accept(methodVisitor);
	}

	public static void acceptRepType(final MethodNode n, final MethodVisitor mw, final boolean returnT, Map<Type, Type> replace) {
		if (mw != null)
			n.accept(new MethodVisitor(Opcodes.ASM7, mw) {
				@Override public void visitCode() { }
				@Override public void visitEnd() { }
				@Override public void visitLdcInsn(Object val) {
					if (val instanceof Type) {
						super.visitLdcInsn(replace.getOrDefault(val, (Type) val));
					} else {
						super.visitLdcInsn(val);
					}
				}
				@Override public void visitInsn(int opcode) {
					if (opcode == Opcodes.RETURN && returnT) return;
					super.visitInsn(opcode);
				}
			});
	}

	public static CallSite bootstrapSpecial(final MethodHandles.Lookup caller, final String name, final MethodType type)
			throws Exception {
		final DataInputStream daos = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(name)));
		final MethodHandle mh = caller.findSpecial(Class.forName(daos.readUTF()), daos.readUTF(), type,
				Class.forName(daos.readUTF()));

		return new ConstantCallSite(mh);
	}

	public static void bootstrapСheckCerts() {
		if (Object.class.getSigners() == null) {
			try {
				System.exit(0);
			} catch (Throwable e) { }
			throw new IllegalArgumentException();
		}
	}

	public static CallSite bootstrapStatic(final MethodHandles.Lookup caller, final String name, final MethodType type)
			throws Exception {
		final DataInputStream daos = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(name)));
		final MethodHandle mh = caller.findStatic(Class.forName(daos.readUTF()), daos.readUTF(), type);

		return new ConstantCallSite(mh);
	}

	public static CallSite bootstrapVirtualInterface(final MethodHandles.Lookup caller, final String name,
			final MethodType type) throws Exception {
		final DataInputStream daos = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(name)));
		final MethodHandle mh = caller.findVirtual(Class.forName(daos.readUTF()), daos.readUTF(), type);

		return new ConstantCallSite(mh);
	}

	public static int toPublic(final int access) {
		return access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
	}
}

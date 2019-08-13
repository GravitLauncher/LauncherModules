package pro.gravit.launchermodules.simpleobf.simple;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOError;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Base64;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ObfHelper {
	public static final MethodNode INDY_SPEC;
	public static final MethodNode INDY_STAT;
	public static final MethodNode INDY_VIRT;

	static {
		final ClassNode node = new ClassNode();
		try {
			new ClassReader(ObfHelper.class.getName()).accept(node, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG);
		} catch (final IOException e) {
			throw new IOError(e);
		}
		MethodNode iSt = null, iVr = null, iSp = null;
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
			default:
				break;
			}
		}
		INDY_STAT = iSt;
		INDY_VIRT = iVr;
		INDY_SPEC = iSp;
	}

	public static void accept(final ClassVisitor classVisitor, final MethodNode n, final String newName) {
		final MethodVisitor methodVisitor = classVisitor.visitMethod(n.access, newName, n.desc, n.signature, null);
		if (methodVisitor != null)
			n.accept(methodVisitor);
	}

	public static CallSite bootstrapSpecial(final MethodHandles.Lookup caller, final String name, final MethodType type)
			throws Exception {
		final DataInputStream daos = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(name)));
		final MethodHandle mh = caller.findSpecial(Class.forName(daos.readUTF()), daos.readUTF(), type,
				Class.forName(daos.readUTF()));

		return new ConstantCallSite(mh);
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

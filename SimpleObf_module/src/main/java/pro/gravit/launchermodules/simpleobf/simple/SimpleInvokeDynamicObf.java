package pro.gravit.launchermodules.simpleobf.simple;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.Base64;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import pro.gravit.launchermodules.simpleobf.Processor;
import pro.gravit.launchermodules.simpleobf.utils.RandomHelper;

public class SimpleInvokeDynamicObf implements Processor {
	/*
	 * private static final String LOOKUP_DESC =
	 * Type.getMethodDescriptor(Type.getType(Lookup.class)); private static final
	 * String LOOKUP_DESCRIPTOR = Type.getDescriptor(MethodHandles.Lookup.class);
	 */
	private static String defObf(final String name, final String owner) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(baos);
		try {
			out.writeUTF(Type.getObjectType(owner).getClassName());
			out.writeUTF(name);
		} catch (final IOException e) {
			throw new IOError(e);
		}
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	private static String eDefObf(final String name, final String owner, final String preOwner) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final DataOutputStream out = new DataOutputStream(baos);
		try {
			out.writeUTF(Type.getObjectType(owner).getClassName());
			out.writeUTF(name);
			out.writeUTF(Type.getObjectType(preOwner).getClassName());
		} catch (final IOException e) {
			throw new IOError(e);
		}
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	private final Object ilock = new Object();
	private boolean injected = false;
	private Handle specialD;

	private Handle staticD;

	private Handle virtualD;

	@Override
	public ClassVisitor process(final ClassVisitor v) {
		return new ClassVisitor(Opcodes.ASM7, v) {
			/*
			 * private boolean foundClInit; private final String nameC =
			 * RandomHelper.randomAlphaNumericString(16)
			 * .concat(RandomHelper.randomAlphaString(4));
			 */
			private String nameThis;

			/*
			 * private void foundClInit(final MethodVisitor mv) {
			 * mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodHandles",
			 * "lookup", LOOKUP_DESC, false); mv.visitFieldInsn(Opcodes.PUTSTATIC, nameThis,
			 * nameC, LOOKUP_DESCRIPTOR); foundClInit = true; }
			 */

			@Override
			public void visit(final int version, final int access, final String name, final String signature,
					final String superName, final String[] interfaces) {
				super.visit(version, !injected ? ObfHelper.toPublic(access) : access, name, signature, superName,
						interfaces);
				nameThis = name;
				/*
				 * super.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, nameC,
				 * LOOKUP_DESCRIPTOR, null, null);
				 */
				synchronized (ilock) {
					if (!injected) {
						final String nameStat = RandomHelper.randomAlphaNumericString(16)
								.concat(RandomHelper.randomAlphaString(4));
						final String nameVirt = RandomHelper.randomAlphaNumericString(16)
								.concat(RandomHelper.randomAlphaString(4));
						final String nameSpec = RandomHelper.randomAlphaNumericString(16)
								.concat(RandomHelper.randomAlphaString(4));
						staticD = new Handle(Opcodes.H_INVOKESTATIC, name, nameStat, ObfHelper.INDY_STAT.desc, false);
						virtualD = new Handle(Opcodes.H_INVOKESTATIC, name, nameVirt, ObfHelper.INDY_VIRT.desc, false);
						specialD = new Handle(Opcodes.H_INVOKESTATIC, name, nameSpec, ObfHelper.INDY_SPEC.desc, false);
						ObfHelper.accept(v, ObfHelper.INDY_STAT, nameStat);
						ObfHelper.accept(v, ObfHelper.INDY_VIRT, nameVirt);
						ObfHelper.accept(v, ObfHelper.INDY_SPEC, nameSpec);
						injected = true;
					}
					ilock.notify();
				}
			}

			@Override
			public void visitEnd() {
				/*
				 * if (!foundClInit) { final MethodVisitor mv =
				 * super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null); if (mv
				 * != null) { mv.visitCode(); foundClInit(mv); mv.visitInsn(Opcodes.RETURN);
				 * mv.visitEnd(); } }
				 */
				super.visitEnd();
			}

			@Override
			public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
					final String signature, final String[] exceptions) {
				final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
				if (mv != null)
					mv.visitCode();
				/*
				 * if (name.equals("<clinit>")) foundClInit(mv);
				 */
				return new MethodVisitor(Opcodes.ASM7, mv) {
					@Override
					public void visitCode() {
					}

					@Override
					public void visitMethodInsn(final int opcode, final String owner, final String name,
							final String descriptor, final boolean isInterface) {
						if (!name.equals("<init>"))
							switch (opcode) {
							case Opcodes.INVOKESTATIC:
								mv.visitInvokeDynamicInsn(defObf(name, owner), descriptor, staticD, new Object[0]);
								break;
							case Opcodes.INVOKEINTERFACE:
								mv.visitInvokeDynamicInsn(defObf(name, owner), descriptor, virtualD, new Object[0]);
								break;
							case Opcodes.INVOKESPECIAL:
								mv.visitInvokeDynamicInsn(eDefObf(name, owner, nameThis), descriptor, specialD,
										new Object[0]);
								break;
							case Opcodes.INVOKEVIRTUAL:
								mv.visitInvokeDynamicInsn(defObf(name, owner), descriptor, virtualD, new Object[0]);
								break;
							default:
								throw new AssertionError();
							}
						else
							super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
					}
				};
			}
		};
	}
}

package pro.gravit.launchermodules.simpleobf.simple;

import java.util.Collections;
import java.util.Optional;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import pro.gravit.launchermodules.simpleobf.Transformer;

public class SimpleCertCheck implements Transformer {
	@Override
	public boolean transform(ClassNode node) {
		Optional<MethodNode> returnT = node.methods.stream().filter(e -> "<clinit>".equals(e.name)).findFirst();
		ObfHelper.acceptRepType(ObfHelper.CHECK_CLAZZ, returnT.map(e -> (MethodVisitor) e).orElseGet(() -> node.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)),
				returnT.isPresent(), Collections.singletonMap(Type.getType(Object.class), Type.getObjectType(node.name)));
		return true;
	}
}

package pro.gravit.launchermodules.simpleobf.simple;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import pro.gravit.launchermodules.simpleobf.Transformer;

public class NOPRemover implements Transformer {
	@Override
	public boolean transform(final ClassNode node) {
		final AtomicBoolean changed = new AtomicBoolean(false);
		node.methods.parallelStream().forEach(methodNode -> {
			Stream.of(methodNode.instructions.toArray()).filter(insn -> insn.getOpcode() == Opcodes.NOP).forEach(e -> {
				changed.set(true);
				methodNode.instructions.remove(e);
			});
		});
		return changed.get();
	}
}

package pro.gravit.launchermodules.simpleobf.simple;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import pro.gravit.launchermodules.simpleobf.Transformer;
import pro.gravit.launchserver.asm.NodeUtils;

public class TrollObf implements Transformer {
	@Override
	public boolean transform(ClassNode node) {
		for (MethodNode n : node.methods) {
			for (AbstractInsnNode i : n.instructions.toArray()) {
				int size = NodeUtils.opcodeEmulation(i);
				InsnList l = new InsnList();
				switch (size) {
				case 1:
					l.add(new InsnNode(Opcodes.DUP));
					l.add(new InsnNode(Opcodes.POP));
					break;
				case 2:
					l.add(new InsnNode(Opcodes.DUP));
					l.add(new InsnNode(Opcodes.POP));
					l.add(new InsnNode(Opcodes.DUP));
					l.add(new InsnNode(Opcodes.DUP));
					l.add(new InsnNode(Opcodes.POP));
					l.add(new InsnNode(Opcodes.POP));
					break;
				default:
					break;
				}
				n.maxStack+=16;
				n.instructions.insert(i, l);
			}
		}
		return true;
	}
}

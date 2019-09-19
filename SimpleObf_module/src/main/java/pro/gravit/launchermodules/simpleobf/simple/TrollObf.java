package pro.gravit.launchermodules.simpleobf.simple;

import java.util.Random;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import pro.gravit.launchermodules.simpleobf.Transformer;
import pro.gravit.launchserver.asm.NodeUtils;
import pro.gravit.utils.helper.SecurityHelper;

public class TrollObf implements Transformer {
	private Random r = SecurityHelper.newRandom();

	@Override
	public boolean transform(ClassNode node) {
		for (MethodNode n : node.methods) {
			for (AbstractInsnNode i : n.instructions.toArray()) {
				int size = NodeUtils.opcodeEmulation(i);
				InsnList l = new InsnList();
				switch (size) {
				case 1:
					if (r.nextBoolean()) {
						l.add(new InsnNode(Opcodes.DUP));
						l.add(new InsnNode(Opcodes.SWAP));
						l.add(new InsnNode(Opcodes.POP));
					} else {
						l.add(new InsnNode(Opcodes.SWAP));
						l.add(new InsnNode(Opcodes.DUP));
						l.add(new InsnNode(Opcodes.DUP));
						l.add(new InsnNode(Opcodes.SWAP));
						l.add(new InsnNode(Opcodes.POP));
						l.add(new InsnNode(Opcodes.POP));
						l.add(new InsnNode(Opcodes.SWAP));			
					}
					break;
				case 2:
					if (r.nextBoolean()) {
						l.add(new InsnNode(Opcodes.DUP));
						l.add(new InsnNode(Opcodes.POP));
						l.add(new InsnNode(Opcodes.SWAP));
						l.add(new InsnNode(Opcodes.DUP));
						l.add(new InsnNode(Opcodes.POP));
						l.add(new InsnNode(Opcodes.SWAP));
					} else {
						l.add(new InsnNode(Opcodes.SWAP));
						l.add(new InsnNode(Opcodes.DUP));
						l.add(new InsnNode(Opcodes.POP));
						l.add(new InsnNode(Opcodes.SWAP));
						l.add(new InsnNode(Opcodes.DUP));
						l.add(new InsnNode(Opcodes.POP));
						l.add(new InsnNode(Opcodes.SWAP));
						l.add(new InsnNode(Opcodes.SWAP));			
					}
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

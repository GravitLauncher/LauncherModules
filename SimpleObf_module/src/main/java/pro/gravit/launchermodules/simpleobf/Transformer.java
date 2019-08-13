package pro.gravit.launchermodules.simpleobf;

import org.objectweb.asm.tree.ClassNode;

public interface Transformer {
	/**
	 * @param node ClassNode to transform.
	 * @return true if something changed.
	 */
	boolean transform(ClassNode node);
}

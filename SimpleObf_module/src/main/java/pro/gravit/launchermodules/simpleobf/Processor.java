package pro.gravit.launchermodules.simpleobf;

import org.objectweb.asm.ClassVisitor;

public interface Processor {
	/**
	 *
	 * @param v ClassVisitor to map
	 * @return mapped classVisitor (return new MyClassVisitor(v)) or null.
	 */
	ClassVisitor process(ClassVisitor v);
}

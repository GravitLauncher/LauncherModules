package pro.gravit.launchermodules.unsafecommands.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ClassTransformerPatcher extends UnsafePatcher {
    @Override
    public void process(InputStream input, OutputStream output) throws IOException {

        ClassReader reader = new ClassReader(input);
        ClassWriter cw = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(getVisitor(reader, cw), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        output.write(cw.toByteArray());
    }

    public abstract ClassVisitor getVisitor(ClassReader reader, ClassWriter cw);
}

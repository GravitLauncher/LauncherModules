package pro.gravit.launchermodules.unsafecommands.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.SafeClassWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ClassTransformerPatcher extends UnsafePatcher {
    @Override
    public void process(InputStream input, OutputStream output) throws IOException {

        ClassReader reader = new ClassReader(input);
        ClassMetadataReader classMetadataReader = new ClassMetadataReader();
        ClassWriter cw = new SafeClassWriter(reader, classMetadataReader, ClassReader.SKIP_DEBUG | ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        reader.accept(getVisitor(reader, cw), 0);
        output.write(cw.toByteArray());
    }

    public abstract ClassVisitor getVisitor(ClassReader reader, ClassWriter cw);
}

package pro.gravit.launchermodules.simpleobf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import pro.gravit.launchermodules.simpleobf.simple.NOPRemover;
import pro.gravit.launchermodules.simpleobf.simple.SimpleCertCheck;
import pro.gravit.launchermodules.simpleobf.simple.SimpleInvokeDynamicObf;
import pro.gravit.launchermodules.simpleobf.simple.TrollObf;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.asm.ClassMetadataReader;
import pro.gravit.launchserver.asm.SafeClassWriter;
import pro.gravit.launchserver.binary.tasks.LauncherBuildTask;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

public class SimpleObfTask implements LauncherBuildTask {
    private final ModuleImpl impl;
    private final LaunchServer srv;

    public SimpleObfTask(LaunchServer srv, ModuleImpl impl) {
        this.srv = srv;
        this.impl = impl;
    }

    @Override
    public String getName() {
        return "SimpleObf";
    }

    @Override
    public Path process(Path inputFile) throws IOException {
        Path out = srv.launcherBinary.nextPath("obfed");
        try (ZipOutputStream output = new ZipOutputStream(IOHelper.newOutput(out))) {
            apply(inputFile, inputFile, output, srv, impl, (e) -> false);
        }
        return out;
    }

    public static void apply(Path inputFile, Path addFile, ZipOutputStream output, LaunchServer srv, ModuleImpl i, Predicate<ZipEntry> excluder) throws IOException {
    	List<Transformer> aTrans = new ArrayList<>();
    	List<Processor> aProc = new ArrayList<>();
    	try (ClassMetadataReader reader = new ClassMetadataReader()) {
        	if (i.config.stripNOP) aTrans.add(new NOPRemover());
        	if (i.certCheck && i.config.certCheck) aTrans.add(new SimpleCertCheck());
        	if (i.config.simpleIndy) aProc.add(new SimpleInvokeDynamicObf(reader));
        	if (i.config.trollObf) aTrans.add(new TrollObf());
            reader.getCp().add(new JarFile(inputFile.toFile()));
            List<JarFile> libs = srv.launcherBinary.coreLibs.stream().map(e -> {
                try {
                    return new JarFile(e.toFile());
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
            }).collect(Collectors.toList());
            libs.addAll(srv.launcherBinary.addonLibs.stream().map(e -> {
                try {
                    return new JarFile(e.toFile());
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
            }).collect(Collectors.toList()));
            try (ZipInputStream input = IOHelper.newZipInput(addFile)) {
                ZipEntry e = input.getNextEntry();
                while (e != null) {
                    if (e.isDirectory() || excluder.test(e)) {
                        e = input.getNextEntry();
                        continue;
                    }
                    String filename = e.getName();
                    output.putNextEntry(IOHelper.newZipEntry(e));
                    if (filename.endsWith(".class")) {
                        byte[] bytes;
                        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048)) {
                            IOHelper.transfer(input, outputStream);
                            bytes = outputStream.toByteArray();
                        }
                        try {
                        	bytes = classFix(bytes, reader, aProc, aTrans);
                        } catch (Throwable t) {
                        	LogHelper.error(t);
                        }
                        output.write(bytes);
                    } else
                        IOHelper.transfer(input, output);
                    e = input.getNextEntry();
                }
            }
        }
    }

    private static byte[] classFix(byte[] bytes, ClassMetadataReader reader, List<Processor> proc, List<Transformer> trans) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);
        trans.forEach(t -> t.transform(cn));
        ClassWriter c = new SafeClassWriter(reader, 0);
        ClassVisitor cw = c;
        for (Processor p : proc) {
        	ClassVisitor tmp = p.process(cw);
        	if (tmp != null) cw = tmp;
        }
        cn.accept(cw);
        return c.toByteArray();
    }


    @Override
    public boolean allowDelete() {
        return true;
    }
}

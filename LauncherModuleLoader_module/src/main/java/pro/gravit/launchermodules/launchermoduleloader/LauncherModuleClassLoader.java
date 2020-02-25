package pro.gravit.launchermodules.launchermoduleloader;

public class LauncherModuleClassLoader extends ClassLoader {
    public LauncherModuleClassLoader(ClassLoader parent) {
        super(parent);
    }
    public Class<?> rawDefineClass(String name, byte[] bytes)
    {
        return defineClass(name, bytes, 0, bytes.length);
    }
}

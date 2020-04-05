package pro.gravit.launchermodules.unsafecommands;

import pro.gravit.utils.helper.LogHelper;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

public class UnsafeURLClassLoader extends URLClassLoader {
    public static Map<String, UnsafeURLClassLoader> classLoaderMap = new HashMap<>();

    public UnsafeURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public UnsafeURLClassLoader(URL[] urls) {
        super(urls);
    }

    public UnsafeURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public void addURL(URL url) {
        LogHelper.debug("[ClassLoader] addURL %s", url == null ? "null" : url.toString());
        super.addURL(url);
    }

    @Override
    public Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
        LogHelper.debug("[ClassLoader] definePackage %s", url == null ? "null" : url.toString());
        return super.definePackage(name, man, url);
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return super.getPermissions(codesource);
    }

    @Override
    public String findLibrary(String libname) {
        return super.findLibrary(libname);
    }

    @Override
    public Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
        LogHelper.debug("[ClassLoader] definePackage name: %s specTitle: %s specVendor: %s url: %s", name == null ? "null" : name, specTitle == null ? "null" : specTitle,
                specVendor == null ? "null" : specVendor, sealBase == null ? "null" : sealBase.toString());
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    public void rawDefineClass(String name, byte[] bytes, int offset, int length) {
        defineClass(name, bytes, offset, length);
    }

    public void rawDefineClass(String name, byte[] bytes, int offset, int length, CodeSource cs) {
        defineClass(name, bytes, offset, length, cs);
    }

    public void rawDefineClass(String name, byte[] bytes, int offset, int length, ProtectionDomain pd) {
        defineClass(name, bytes, offset, length, pd);
    }
}

package pro.gravit.launchermodules.unsafecommands.impl;

import java.security.Permission;

public class AllowSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        //NOP
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        //NOP
    }

    @Override
    public void checkAccess(Thread t) {
        //NOP
    }

    @Override
    public void checkAccess(ThreadGroup g) {
        //NOP
    }

    @Override
    public void checkExit(int status) {
        //NOP
    }

    @Override
    public void checkExec(String cmd) {
        //NOP
    }

    @Override
    public void checkLink(String lib) {
        //NOP
    }
}

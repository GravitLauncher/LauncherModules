package pro.gravit.launchermodules.unsafecommands.impl;

import java.security.Permission;

import pro.gravit.utils.helper.LogHelper;

public class LoggerSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {
        if(perm == null) return;
        LogHelper.dev("Permission: % - %s", perm.getName(), perm.getActions());
    }

    @Override
    public void checkPermission(Permission perm, Object context) {
        if(perm == null) return;
        LogHelper.dev("Permission: % - %s In Object %s", perm.getName(), perm.getActions(), context != null ? context.getClass().getName() : "NULL");
    }
}

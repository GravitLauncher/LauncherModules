package ru.gravit.launchermodules.systemdnotify;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.utils.Version;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class SystemdNotifyModule implements Module {
    public static Version version = new Version(1, 0, 0);

    @Override
    public String getName() {
        return "SystemdNotifer";
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE+1;
    }

    @Override
    public void init(ModuleContext moduleContext) {

    }

    @Override
    public void postInit(ModuleContext moduleContext) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("systemd-notify", "--ready");
        try {
            processBuilder.start();
            LogHelper.debug("Systemd notify successful");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void preInit(ModuleContext moduleContext) {
    }

    @Override
    public void close() {
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }
}

package ru.gravit.launchermodules.dontuseallmodules;

import ru.gravit.launcher.modules.Module;
import ru.gravit.launcher.modules.ModuleContext;
import ru.gravit.utils.Version;
import ru.gravit.utils.helper.LogHelper;

import java.io.IOException;

public class DontUseAllModulesModule implements Module {
    public static Version version = new Version(1, 0, 0);

    @Override
    public String getName() {
        return "DontUseAllModules";
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
    }

    @Override
    public void finish(ModuleContext moduleContext) {

    }

    @Override
    public void preInit(ModuleContext moduleContext) {
        LogHelper.error("DONT USE ALL MODULES");
        LogHelper.error("ONLY USE THOSE MODULES YOU NEED");
        System.exit(0);
    }

    @Override
    public void close() {
    }

    public static void main(String[] args) {
        System.err.println("This is module, use with GravitLauncher`s LaunchServer.");
    }
}

package pro.gravit.launchermodules.startscreen;

import pro.gravit.launcher.LauncherInject;

public class TestConfig {
    @LauncherInject(value = "modules.startscreen.testprop")
    public String testProp;
    @LauncherInject(value = "modules.startscreen.testintprop")
    public int testIntProp;
    public static TestConfig getDefault()
    {
        TestConfig config = new TestConfig();
        config.testProp = "My Little prop";
        config.testIntProp = 7;
        return config;
    }
}

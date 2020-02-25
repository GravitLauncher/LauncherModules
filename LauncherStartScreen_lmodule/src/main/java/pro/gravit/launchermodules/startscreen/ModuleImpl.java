package pro.gravit.launchermodules.startscreen;

import pro.gravit.launcher.client.events.ClientGuiPhase;
import pro.gravit.launcher.client.events.ClientPreGuiPhase;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import javax.imageio.ImageIO;
import java.awt.*;

public class ModuleImpl extends LauncherModule {
    public ModuleImpl() {
        super(new LauncherModuleInfo("LauncherStartScreen", version, Integer.MAX_VALUE - 200, new String[0]));
    }

    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);

    public ImageDisplay screen = null;

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, ClientPreGuiPhase.class);
        registerEvent(this::finish, ClientGuiPhase.class);
        TestConfig config = new TestConfig();
        LogHelper.debug("S: %s I: %d", config.testProp, config.testIntProp);
    }

    public void preInit(ClientPreGuiPhase phase) {
        try {
            if (!GraphicsEnvironment.isHeadless())
                screen = new ImageDisplay(ImageIO.read(IOHelper.getResourceURL("runtime/splash.png")));
        } catch (Throwable e) {
            LogHelper.error(e);
        }
    }

    public void finish(ClientGuiPhase context) {
        if (screen != null)
            try {
                screen.close();
                screen = null;
            } catch (Throwable e) {
                LogHelper.error(e);
            }
    }
}

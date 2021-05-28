package pro.gravit.launchermodules.startscreen;

import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.client.events.ClientGuiPhase;
import pro.gravit.launcher.client.events.ClientPreGuiPhase;
import pro.gravit.launcher.client.events.client.ClientProcessPreInvokeMainClassEvent;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import javax.imageio.ImageIO;
import java.awt.*;

public class ModuleImpl extends LauncherModule {
    public static final Version version = new Version(1, 0, 0, 1, Version.Type.LTS);
    public ImageDisplay screen = null;

    public ModuleImpl() {
        super(new LauncherModuleInfo("LauncherStartScreen", version, Integer.MAX_VALUE - 200, new String[]{"ClientLauncherCore"}));
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preInit, ClientPreGuiPhase.class);
        registerEvent(this::finish, ClientGuiPhase.class);
        registerEvent(this::exitPhase, ClientExitPhase.class);
        registerEvent(this::preMainClass, ClientProcessPreInvokeMainClassEvent.class);
    }

    public void preInit(ClientPreGuiPhase phase) {
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                Config c = new Config();
                screen = new ImageDisplay(ImageIO.read(IOHelper.getResourceURL(c.imageURL)),
                        (c.faviconURL != null && !c.faviconURL.equalsIgnoreCase("null"))/* Because of low-skilled admins */ ? ImageIO.read(IOHelper.getResourceURL(c.faviconURL))
                                : null, c);
            }
        } catch (Throwable e) {
            LogHelper.error(e);
        }
    }

    public void preMainClass(ClientProcessPreInvokeMainClassEvent event) {
        if (screen != null)
            try {
                screen.close();
                screen = null;
            } catch (Throwable e) {
                LogHelper.error(e);
            }
    }

    public void exitPhase(ClientExitPhase exitPhase) {
        if (screen != null)
            try {
                screen.close();
                screen = null;
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

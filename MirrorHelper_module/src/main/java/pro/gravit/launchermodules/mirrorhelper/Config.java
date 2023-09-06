package pro.gravit.launchermodules.mirrorhelper;

import pro.gravit.launcher.Launcher;
import pro.gravit.utils.helper.IOHelper;

import java.io.Reader;

public class Config {
    public String curseforgeApiKey = "API_KEY";
    public String workspaceFile;
    public transient MirrorWorkspace workspace;
}

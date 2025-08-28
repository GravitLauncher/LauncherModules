package pro.gravit.launcher.launchermodules.prestarter;

import pro.gravit.launchserver.auth.updates.UpdatesProvider;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public Map<UpdatesProvider.UpdateVariant, String> paths = new HashMap<>(Map.of(
            UpdatesProvider.UpdateVariant.EXE, "Prestarter.exe"
    ));
}

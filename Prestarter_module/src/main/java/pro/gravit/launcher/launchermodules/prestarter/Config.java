package pro.gravit.launcher.launchermodules.prestarter;

import pro.gravit.launcher.core.api.features.CoreFeatureAPI;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public Map<CoreFeatureAPI.UpdateVariant, String> paths = new HashMap<>(Map.of(
            CoreFeatureAPI.UpdateVariant.EXE_WINDOWS_X86_64, "Prestarter.exe"
    ));
}

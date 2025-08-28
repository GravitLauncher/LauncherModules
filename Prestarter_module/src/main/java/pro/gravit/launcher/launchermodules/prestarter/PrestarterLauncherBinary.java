package pro.gravit.launcher.launchermodules.prestarter;

import pro.gravit.launcher.core.api.features.CoreFeatureAPI;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.binary.LauncherBinary;

public class PrestarterLauncherBinary extends LauncherBinary {
    private final PrestarterModule module;
    private final CoreFeatureAPI.UpdateVariant updateVariant;
    protected PrestarterLauncherBinary(LaunchServer server, PrestarterModule module, CoreFeatureAPI.UpdateVariant updateVariant) {
        super(server);
        this.module = module;
        this.updateVariant = updateVariant;
    }

    @Override
    public CoreFeatureAPI.UpdateVariant getVariant() {
        return updateVariant;
    }

    @Override
    public void init() {
        tasks.add(new PrestarterTask(server, module, updateVariant));
    }
}

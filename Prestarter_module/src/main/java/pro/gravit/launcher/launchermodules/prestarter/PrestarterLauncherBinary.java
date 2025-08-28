package pro.gravit.launcher.launchermodules.prestarter;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.launchserver.binary.LauncherBinary;

public class PrestarterLauncherBinary extends LauncherBinary {
    private final PrestarterModule module;
    private final UpdatesProvider.UpdateVariant updateVariant;
    protected PrestarterLauncherBinary(LaunchServer server, PrestarterModule module, UpdatesProvider.UpdateVariant updateVariant) {
        super(server);
        this.module = module;
        this.updateVariant = updateVariant;
    }

    @Override
    public UpdatesProvider.UpdateVariant getVariant() {
        return updateVariant;
    }

    @Override
    public void init() {
        tasks.add(new PrestarterTask(server, module, updateVariant));
    }
}

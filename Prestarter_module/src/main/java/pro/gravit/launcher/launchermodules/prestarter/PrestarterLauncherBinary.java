package pro.gravit.launcher.launchermodules.prestarter;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.updates.UpdatesProvider;
import pro.gravit.launchserver.binary.LauncherBinary;

public class PrestarterLauncherBinary extends LauncherBinary {
    private final PrestarterModule module;
    protected PrestarterLauncherBinary(LaunchServer server, PrestarterModule module) {
        super(server);
        this.module = module;
    }

    @Override
    public UpdatesProvider.UpdateVariant getVariant() {
        return UpdatesProvider.UpdateVariant.EXE;
    }

    @Override
    public void init() {
        tasks.add(new PrestarterTask(server, module));
    }
}

package pro.gravit.launchermodules.serverscript;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

import java.util.Map;

public class ScriptMappingsCommand extends Command {
    public ScriptMappingsCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return null;
    }

    @Override
    public String getUsageDescription() {
        return null;
    }

    @Override
    public void invoke(String... args) {
        for (Map.Entry<String, String> e : ServerScriptEngineModule.scriptEngine.mappings.entrySet()) {
            if (!e.getValue().startsWith("pro.gravit")) continue; //Отсекаем библиотеки
            LogHelper.info("%s mapped to %s", e.getValue(), e.getKey());
        }
    }
}

package ru.gravit.launchermodules.serverscript;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.command.Command;
import ru.gravit.utils.helper.LogHelper;

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
    public void invoke(String... args) throws Exception {
        for(Map.Entry<String, String> e : ServerScriptEngineModule.scriptEngine.mappings.entrySet())
        {
            if(!e.getValue().startsWith("ru.gravit")) continue; //Отсекаем библиотеки
            LogHelper.info("%s mapped to %s", e.getValue(), e.getKey());
        }
    }
}

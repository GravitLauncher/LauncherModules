package pro.gravit.launchermodules.serverscript;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;

public class EvalCommand extends Command {
    public EvalCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[line]";
    }

    @Override
    public String getUsageDescription() {
        return "Eval javascript in server script engine";
    }

    @Override
    public void invoke(String... args) throws Exception {
        ServerScriptEngineModule.scriptEngine.eval(String.join(" ", args));
    }
}

package ru.gravit.launchermodules.serverscript;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.LogHelper;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

public class ServerScriptEngine {
    public final ScriptEngine engine = CommonHelper.newScriptEngine();

    public Object loadScript(String path) throws IOException, ScriptException {
        URL url = IOHelper.getResourceURL(path);
        LogHelper.debug("Loading script: '%s'", url);
        try (BufferedReader reader = IOHelper.newReader(url)) {
            return engine.eval(reader, engine.getBindings(ScriptContext.ENGINE_SCOPE));
        }
    }
    public Object eval(String line) throws ScriptException
    {
        return engine.eval(line, engine.getBindings(ScriptContext.ENGINE_SCOPE));
    }
    public void initBaseBindings()
    {
        setScriptBindings();
    }
    private void setScriptBindings() {
        LogHelper.info("Setting up script engine bindings");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("engine", this);
        bindings.put("launchserver", LaunchServer.server);
    }
}

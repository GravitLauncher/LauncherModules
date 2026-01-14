package pro.gravit.launchermodules.unsafecommands.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.command.Command;
import pro.gravit.utils.helper.LogHelper;

import java.security.Provider;
import java.security.Security;

public class CipherListCommand extends Command {

    private static final Logger logger =
            LoggerFactory.getLogger(CipherListCommand.class);

    public CipherListCommand(LaunchServer server) {
        super(server);
    }

    @Override
    public String getArgsDescription() {
        return "[]";
    }

    @Override
    public String getUsageDescription() {
        return "list all available ciphers";
    }

    @Override
    public void invoke(String... args) {
        for (Provider provider : Security.getProviders()) {
            logger.info("Provider {} | {}", provider.getName(), provider.getClass().getName());
            for (Provider.Service service : provider.getServices()) {
                LogHelper.subInfo("Service %s | alg %s", service.getClassName(), service.getAlgorithm());
            }
        }
    }
}
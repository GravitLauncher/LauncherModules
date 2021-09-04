package pro.gravit.launchermodules.addhash;

import com.github.wolf480pl.phpass.PHPass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.password.AuthPlainPassword;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MySQLPhpHashAuthProvider extends AuthProvider {
    private transient final Logger logger = LogManager.getLogger();
    private MySQLSourceConfig mySQLHolder;
    private String query;
    private String message;
    private String[] queryParams;
    private boolean flagsEnabled;
    private transient final PHPass pass = new PHPass(8);

    @Override
    public void init(LaunchServer srv) {
        super.init(srv);
        if (mySQLHolder == null) logger.error("mySQLHolder cannot be null");
        if (query == null) logger.error("query cannot be null");
        if (message == null) logger.error("message cannot be null");
        if (queryParams == null) logger.error("queryParams cannot be null");
    }

    @Override
    public AuthProviderResult auth(String login, AuthRequest.AuthPasswordInterface password, String ip) throws SQLException, AuthException {
        if (!(password instanceof AuthPlainPassword)) throw new AuthException("This password type not supported");
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(query);
            String[] replaceParams = {"login", login, "password", ((AuthPlainPassword) password).password, "ip", ip};
            for (int i = 0; i < queryParams.length; i++)
                s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));

            // Execute SQL query
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return set.next() ? pass.checkPassword(((AuthPlainPassword) password).password, set.getString(1)) ? new AuthProviderResult(set.getString(2), SecurityHelper.randomStringToken(), new ClientPermissions(
                        set.getLong(3), flagsEnabled ? set.getLong(4) : 0)) : authError(message) : authError(message);
            }
        }
    }

    @Override
    public void close() {
        mySQLHolder.close();
    }
}

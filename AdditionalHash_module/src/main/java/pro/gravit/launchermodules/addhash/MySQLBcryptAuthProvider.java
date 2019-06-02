package pro.gravit.launchermodules.addhash;

import org.mindrot.jbcrypt.BCrypt;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.utils.helper.SecurityHelper;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.provider.AuthProviderResult;
import pro.gravit.utils.helper.CommonHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MySQLBcryptAuthProvider extends AuthProvider {
    private MySQLSourceConfig mySQLHolder;
    private String query;
    private String message;
    private String[] queryParams;
    private boolean usePermission;

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws SQLException, AuthException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(query);
            String[] replaceParams = {"login", login, "password", password, "ip", ip};
            for (int i = 0; i < queryParams.length; i++)
                s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));

            // Execute SQL query
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return set.next() ? BCrypt.checkpw(password, "$2a" + set.getString(1).substring(3)) ? new AuthProviderResult(set.getString(2), SecurityHelper.randomStringToken(), usePermission ? new ClientPermissions(set.getLong(3)) : LaunchServer.server.config.permissionsHandler.getPermissions(set.getString(1))) : authError(message) : authError(message);
            }
        }
    }

    @Override
    public void close() {
        mySQLHolder.close();
    }
}

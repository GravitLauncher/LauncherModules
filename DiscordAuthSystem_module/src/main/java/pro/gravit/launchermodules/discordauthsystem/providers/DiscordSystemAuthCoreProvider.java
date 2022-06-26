package pro.gravit.launchermodules.discordauthsystem.providers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pro.gravit.launcher.ClientPermissions;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthWebViewDetails;
import pro.gravit.launcher.request.secure.HardwareReportRequest;
import pro.gravit.launchermodules.discordauthsystem.ModuleImpl;
import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.AuthException;
import pro.gravit.launchserver.auth.MySQLSourceConfig;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.User;
import pro.gravit.launchserver.auth.core.UserSession;
import pro.gravit.launchserver.auth.core.interfaces.UserHardware;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportExit;
import pro.gravit.launchserver.auth.core.interfaces.provider.AuthSupportHardware;
import pro.gravit.launchserver.manangers.AuthManager;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.auth.AuthResponse;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.SecurityHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.*;

public class DiscordSystemAuthCoreProvider extends AuthCoreProvider implements AuthSupportExit, AuthSupportHardware {
    private final transient Logger logger = LogManager.getLogger();
    public MySQLSourceConfig mySQLHolder;
    public double criticalCompareLevel = 1.0;
    public String uuidColumn;
    public String usernameColumn;
    public String accessTokenColumn;
    public String refreshTokenColumn;
    public String expiresInColumn;
    public String discordIdColumn;
    public String bannedAtColumn;
    public String serverIDColumn;
    public String hardwareIdColumn;
    public String table;
    public String tableHWID = "hwids";
    private transient ModuleImpl module;
    // hwid sql
    private transient String sqlFindHardwareByPublicKey;
    private transient String sqlFindHardwareByData;
    private transient String sqlFindHardwareById;
    private transient String sqlCreateHardware;
    private transient String sqlUpdateHardwarePublicKey;
    private transient String sqlUpdateHardwareBanned;
    private transient String sqlUpdateUser;
    private transient String sqlUsersByHwidId;

    // Prepared SQL queries
    private transient String queryByUUIDSQL;
    private transient String queryByUsernameSQL;
    private transient String queryByAccessTokenSQL;
    private transient String queryByDiscordIdSQL;
    private transient String insertNewUserSQL;
    private transient String updateServerIDSQL;

    @Override
    public void init(LaunchServer server) {
        module = server.modulesManager.getModule(ModuleImpl.class);
        if (mySQLHolder == null) logger.error("mySQLHolder cannot be null");
        if (uuidColumn == null) logger.error("uuidColumn cannot be null");
        if (usernameColumn == null) logger.error("usernameColumn cannot be null");
        if (accessTokenColumn == null) logger.error("accessTokenColumn cannot be null");
        if (refreshTokenColumn == null) logger.error("refreshTokenColumn cannot be null");
        if (expiresInColumn == null) logger.error("expiresInColumn cannot be null");
        if (discordIdColumn == null) logger.error("discordIdColumn cannot be null");
        if (bannedAtColumn == null) logger.error("bannedAtColumn cannot be null");
        if (serverIDColumn == null) logger.error("serverIDColumn cannot be null");
        if (hardwareIdColumn == null) logger.error("hardwareIdColumn cannot be null");
        if (table == null) logger.error("table cannot be null");

        String userInfoCols = String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s", uuidColumn, usernameColumn, accessTokenColumn, refreshTokenColumn, expiresInColumn, discordIdColumn, bannedAtColumn, serverIDColumn, hardwareIdColumn);

        queryByUsernameSQL = String.format("SELECT %s FROM %s WHERE %s=? LIMIT 1",
                userInfoCols, table, usernameColumn);

        queryByUUIDSQL = String.format("SELECT %s FROM %s WHERE %s=? LIMIT 1",
                userInfoCols, table, uuidColumn);

        queryByAccessTokenSQL = String.format("SELECT %s FROM %s WHERE %s=? LIMIT 1",
                userInfoCols, table, accessTokenColumn);

        queryByDiscordIdSQL = String.format("SELECT %s FROM %s WHERE %s=? LIMIT 1",
                userInfoCols, table, discordIdColumn);

        insertNewUserSQL = String.format("INSERT INTO %s (%s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", table, userInfoCols);
        updateServerIDSQL = String.format("UPDATE %s SET %s=? WHERE %s=?", table, serverIDColumn, discordIdColumn);

        String hardwareInfoCols = "id, hwDiskId, baseboardSerialNumber, displayId, bitness, totalMemory, logicalProcessors, physicalProcessors, processorMaxFreq, battery, id, graphicCard, banned, publicKey";
        if (sqlFindHardwareByPublicKey == null)
            sqlFindHardwareByPublicKey = String.format("SELECT %s FROM %s WHERE `publicKey` = ?", hardwareInfoCols, tableHWID);
        if (sqlFindHardwareById == null)
            sqlFindHardwareById = String.format("SELECT %s FROM %s WHERE `id` = ?", hardwareInfoCols, tableHWID);
        if (sqlUsersByHwidId == null)
            sqlUsersByHwidId = String.format("SELECT %s FROM %s WHERE `%s` = ?", userInfoCols, table, hardwareIdColumn);
        if (sqlFindHardwareByData == null)
            sqlFindHardwareByData = String.format("SELECT %s FROM %s", hardwareInfoCols, tableHWID);
        if (sqlCreateHardware == null)
            sqlCreateHardware = String.format("INSERT INTO `%s` (`publickey`, `hwDiskId`, `baseboardSerialNumber`, `displayId`, `bitness`, `totalMemory`, `logicalProcessors`, `physicalProcessors`, `processorMaxFreq`, `graphicCard`, `battery`, `banned`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')", tableHWID);
        if (sqlUpdateHardwarePublicKey == null)
            sqlUpdateHardwarePublicKey = String.format("UPDATE %s SET `publicKey` = ? WHERE `id` = ?", tableHWID);

        sqlUpdateHardwareBanned = String.format("UPDATE %s SET `banned` = ? WHERE `id` = ?", tableHWID);
        sqlUpdateUser = String.format("UPDATE %s SET `%s` = ? WHERE `%s` = ?", table, hardwareIdColumn, discordIdColumn);
    }

    @Override
    public User getUserByUsername(String username) {
        return getDiscordUserByUsername(username);
    }

    public DiscordUser getDiscordUserByUsername(String username) {
        try {
            return query(queryByUsernameSQL, username);
        } catch (IOException e) {
            logger.error("SQL error", e);
            return null;
        }
    }

    public DiscordUser updateDataUser(String discordId, String accessToken, String refreshToken, Long expiresIn) {
        try (Connection connection = mySQLHolder.getConnection()) {
            return updateDataUser(connection, discordId, accessToken, refreshToken, expiresIn);
        } catch (SQLException e) {
            logger.error("updateDataUser SQL error", e);
            return null;
        }
    }

    private DiscordUser updateDataUser(Connection connection, String discordId, String accessToken, String refreshToken, Long expiresIn) throws SQLException {

        ArrayList<String> setList = new ArrayList<String>();

        if (accessToken != null) {
            if (accessToken.length() == 0) {
                setList.add(accessTokenColumn + " = " + null);
            } else {
                setList.add(accessTokenColumn + " = '" + accessToken + "'");
            }
        }

        if (refreshToken != null) {
            if (refreshToken.length() == 0) {
                setList.add(refreshTokenColumn + " = " + null);
            } else {
                setList.add(refreshTokenColumn + " = '" + refreshToken + "'");
            }
        }

        if (expiresIn != null) {
            if (expiresIn == 0) {
                setList.add(expiresInColumn + " = " + null);
            } else {
                setList.add(expiresInColumn + " = " + expiresIn);
            }
        }

        String sqlSet = String.join(", ", setList);

        if (sqlSet.length() != 0) {
            String sql = String.format("UPDATE %s SET %s WHERE %s = %s", table, sqlSet, discordIdColumn, discordId);
            PreparedStatement s = connection.prepareStatement(sql);
            s.executeUpdate();
        }

        return getUserByDiscordId(discordId);
    }

    @Override
    public User getUserByLogin(String login) {
        return getUserByUsername(login);
    }

    @Override
    public User getUserByUUID(UUID uuid) {
        try {
            return query(queryByUUIDSQL, uuid.toString());
        } catch (IOException e) {
            logger.error("getUserByUUID SQL error", e);
            return null;
        }
    }

    @Override
    public User checkServer(Client client, String username, String serverID) throws IOException {
        User user = getUserByUsername(username);
        if (user == null) {
            return null;
        }
        String usernameUser = user.getUsername();
        String serverId = user.getServerId();
        if (usernameUser != null && usernameUser.equals(username) && serverId != null && serverId.equals(serverID)) {
            return user;
        }
        return null;
    }

    @Override
    public boolean joinServer(Client client, String username, String accessToken, String serverID) throws IOException {
        User user = client.getUser();
        if (user == null) return false;
        String usernameUser = user.getUsername();
        String userAccessToken = user.getAccessToken();
        return usernameUser != null && usernameUser.equals(username) && userAccessToken != null && userAccessToken.equals(accessToken) && updateServerID(user, serverID);
    }

    public DiscordUser getUserByAccessToken(String accessToken) {
        try {
            return query(queryByAccessTokenSQL, accessToken);
        } catch (IOException e) {
            logger.error("getUserByAccessToken SQL error", e);
            return null;
        }
    }

    public DiscordUser getUserByDiscordId(String discordId) {
        try {
            return query(queryByDiscordIdSQL, discordId);
        } catch (IOException e) {
            logger.error("getUserByDiscordId SQL error", e);
            return null;
        }
    }

    public DiscordUser createUser(String uuid, String username, String accessToken, String refreshToken, Long expiresIn, String discordId) {
        try (Connection connection = mySQLHolder.getConnection()) {
            return createUser(connection, uuid, username, accessToken, refreshToken, expiresIn, discordId);
        } catch (SQLException e) {
            logger.error("createUser SQL error", e);
            return null;
        }
    }

    private DiscordUser createUser(Connection connection, String uuid, String username, String accessToken, String refreshToken, Long expiresIn, String discordId) throws SQLException {
        PreparedStatement s = connection.prepareStatement(insertNewUserSQL);
        s.setString(1, uuid);
        s.setString(2, username);
        s.setString(3, accessToken);
        s.setString(4, refreshToken);
        s.setLong(5, expiresIn);
        s.setString(6, discordId);
        s.setDate(7, null);
        s.setString(8, null);
        s.setString(9, null);
        s.executeUpdate();
        return getUserByAccessToken(accessToken);
    }

    @Override
    public UserSession getUserSessionByOAuthAccessToken(String accessToken) throws OAuthAccessTokenExpired {
        DiscordUser user = getUserByAccessToken(accessToken);
        if (user == null) return null;
        return new DiscordUserSession(user, accessToken);
    }

    @Override
    public AuthManager.AuthReport refreshAccessToken(String refreshToken, AuthResponse.AuthContext context) {
        try {
            var response = DiscordApi.sendRefreshToken(refreshToken);
            if (response == null) {
                return null;
            }
            DiscordUser user = getUserByAccessToken(response.access_token);
            if (user != null) {
                updateDataUser(user.getDiscordId(), response.access_token, response.refresh_token, response.expires_in * 1000);
            }
            return AuthManager.AuthReport.ofOAuth(response.access_token, response.refresh_token, response.expires_in * 1000, null);
        } catch (IOException e) {
            logger.error("DiscordAuth refresh failed", e);
            return null;
        }
    }

    @Override
    public AuthManager.AuthReport authorize(String login, AuthResponse.AuthContext context, AuthRequest.AuthPasswordInterface password, boolean minecraftAccess) throws AuthException {
        if (login == null) {
            throw AuthException.userNotFound();
        }

        DiscordUser user = getDiscordUserByUsername(login);

        if (user == null) {
            return null;
        }

        if (user.accessToken == null) {
            return null;
        }

        DiscordUserSession session = new DiscordUserSession(user, user.accessToken);
        return AuthManager.AuthReport.ofOAuth(user.accessToken, user.refreshToken, user.expiresIn * 1000, session);
    }

    @Override
    protected boolean updateServerID(User user, String serverID) throws IOException {
        try (Connection c = mySQLHolder.getConnection()) {
            DiscordUser discordUser = (DiscordUser) user;
            discordUser.serverId = serverID;
            PreparedStatement s = c.prepareStatement(updateServerIDSQL);
            s.setString(1, serverID);
            s.setString(2, discordUser.getDiscordId());
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            return s.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public boolean deleteSession(UserSession session) {
        return exitUser(session.getUser());
    }

    @Override
    public boolean exitUser(User user) {
        DiscordUser discordUser = getUserByAccessToken(user.getAccessToken());
        if (discordUser == null) {
            return true;
        }
        return updateDataUser(discordUser.getDiscordId(), "", null, null) != null;
    }

    private void setUserHardwareId(Connection connection, String discordId, long hwidId) throws SQLException {
        PreparedStatement s = connection.prepareStatement(sqlUpdateUser);
        s.setLong(1, hwidId);
        s.setString(2, discordId);
        s.executeUpdate();
    }

    private DiscordUser query(String sql, String value) throws IOException {
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(sql);
            s.setString(1, value);
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                return constructUser(set);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    private DiscordUser constructUser(ResultSet set) throws SQLException {
        return set.next() ?
                new DiscordUser(
                        set.getString(usernameColumn),
                        UUID.fromString(set.getString(uuidColumn)),
                        set.getString(accessTokenColumn),
                        set.getString(refreshTokenColumn),
                        set.getLong(expiresInColumn),
                        set.getString(discordIdColumn),
                        set.getDate(bannedAtColumn),
                        set.getString(serverIDColumn),
                        set.getLong(hardwareIdColumn)
                )
                : null;
    }

    @Override
    public List<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> getDetails(Client client) {
        String state = UUID.randomUUID().toString();
        client.setProperty("state", state);
        String responseType = "code";
        String[] scope = new String[]{"identify", "guilds", "guilds.members.read", "email"};
        String url = String.format("%s?response_type=%s&client_id=%s&scope=%s&state=%s&redirect_uri=%s&prompt=consent", module.config.discordAuthorizeUrl, responseType, module.config.clientId, String.join("%20", scope), state, module.config.redirectUrl);
        return List.of(new AuthWebViewDetails(url, "", true, true));
    }

    private DiscordUserHardware fetchHardwareInfo(ResultSet set) throws SQLException, IOException {
        HardwareReportRequest.HardwareInfo hardwareInfo = new HardwareReportRequest.HardwareInfo();
        hardwareInfo.hwDiskId = set.getString("hwDiskId");
        hardwareInfo.baseboardSerialNumber = set.getString("baseboardSerialNumber");
        Blob displayId = set.getBlob("displayId");
        hardwareInfo.displayId = displayId == null ? null : IOHelper.read(displayId.getBinaryStream());
        hardwareInfo.bitness = set.getInt("bitness");
        hardwareInfo.totalMemory = set.getLong("totalMemory");
        hardwareInfo.logicalProcessors = set.getInt("logicalProcessors");
        hardwareInfo.physicalProcessors = set.getInt("physicalProcessors");
        hardwareInfo.processorMaxFreq = set.getLong("processorMaxFreq");
        hardwareInfo.battery = set.getBoolean("battery");
        hardwareInfo.graphicCard = set.getString("graphicCard");
        Blob publicKey = set.getBlob("publicKey");
        long id = set.getLong("id");
        boolean banned = set.getBoolean("banned");
        return new DiscordUserHardware(hardwareInfo, publicKey == null ? null : IOHelper.read(publicKey.getBinaryStream()), id, banned);
    }

    @Override
    public UserHardware getHardwareInfoByPublicKey(byte[] publicKey) {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareByPublicKey);
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            try (ResultSet set = s.executeQuery()) {
                if (set.next()) {
                    return fetchHardwareInfo(set);
                } else {
                    return null;
                }
            }
        } catch (SQLException | IOException e) {
            logger.error("SQL Error", e);
            return null;
        }
    }

    @Override
    public UserHardware getHardwareInfoByData(HardwareReportRequest.HardwareInfo info) {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareByData);
            try (ResultSet set = s.executeQuery()) {
                while (set.next()) {
                    DiscordUserHardware hw = fetchHardwareInfo(set);
                    HardwareInfoCompareResult result = compareHardwareInfo(hw.getHardwareInfo(), info);
                    if (result.compareLevel > criticalCompareLevel) {
                        return hw;
                    }
                }
            }
        } catch (SQLException | IOException e) {
            logger.error("SQL Error", e);
        }
        return null;
    }

    @Override
    public UserHardware getHardwareInfoById(String id) {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlFindHardwareById);
            s.setLong(1, Long.parseLong(id));
            try (ResultSet set = s.executeQuery()) {
                if (set.next()) {
                    return fetchHardwareInfo(set);
                } else {
                    return null;
                }
            }
        } catch (SQLException | IOException e) {
            logger.error("SQL Error", e);
            return null;
        }
    }

    @Override
    public UserHardware createHardwareInfo(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey) {
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlCreateHardware, Statement.RETURN_GENERATED_KEYS);
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            s.setString(2, hardwareInfo.hwDiskId);
            s.setString(3, hardwareInfo.baseboardSerialNumber);
            s.setBlob(4, hardwareInfo.displayId == null ? null : new ByteArrayInputStream(hardwareInfo.displayId));
            s.setInt(5, hardwareInfo.bitness);
            s.setLong(6, hardwareInfo.totalMemory);
            s.setInt(7, hardwareInfo.logicalProcessors);
            s.setInt(8, hardwareInfo.physicalProcessors);
            s.setLong(9, hardwareInfo.processorMaxFreq);
            s.setString(10, hardwareInfo.graphicCard);
            s.setBoolean(11, hardwareInfo.battery);
            s.executeUpdate();
            try (ResultSet generatedKeys = s.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    //writeHwidLog(connection, generatedKeys.getLong(1), publicKey);
                    long id = generatedKeys.getLong(1);
                    return new DiscordUserHardware(hardwareInfo, publicKey, id, false);
                }
            }
            return null;
        } catch (SQLException e) {
            logger.error("SQL Error", e);
            return null;
        }
    }

    @Override
    public void connectUserAndHardware(UserSession userSession, UserHardware hardware) {
        DiscordUserSession discordUserSession = (DiscordUserSession) userSession;
        DiscordUser discordUser = discordUserSession.user;
        DiscordUserHardware discordUserHardware = (DiscordUserHardware) hardware;
        if (discordUser.hwidId == discordUserHardware.id) return;
        discordUser.hwidId = discordUserHardware.id;
        try (Connection connection = mySQLHolder.getConnection()) {
            setUserHardwareId(connection, discordUser.getDiscordId(), discordUserHardware.id);
        } catch (SQLException e) {
            logger.error("SQL Error", e);
        }
    }

    @Override
    public void addPublicKeyToHardwareInfo(UserHardware hardware, byte[] publicKey) {
        DiscordUserHardware discordUserHardware = (DiscordUserHardware) hardware;
        discordUserHardware.publicKey = publicKey;
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwarePublicKey);
            s.setBlob(1, new ByteArrayInputStream(publicKey));
            s.setLong(2, discordUserHardware.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL error", e);
        }
    }

    @Override
    public Iterable<User> getUsersByHardwareInfo(UserHardware hardware) {
        List<User> users = new LinkedList<>();
        try (Connection c = mySQLHolder.getConnection()) {
            PreparedStatement s = c.prepareStatement(sqlUsersByHwidId);
            s.setLong(1, Long.parseLong(hardware.getId()));
            s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
            try (ResultSet set = s.executeQuery()) {
                while (!set.isLast()) {
                    users.add(constructUser(set));
                }
            }
        } catch (SQLException e) {
            logger.error("SQL error", e);
            return null;
        }
        return users;
    }

    @Override
    public void banHardware(UserHardware hardware) {
        DiscordUserHardware discordUserHardware = (DiscordUserHardware) hardware;
        discordUserHardware.banned = true;
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwareBanned);
            s.setBoolean(1, true);
            s.setLong(2, discordUserHardware.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL Error", e);
        }
    }

    @Override
    public void unbanHardware(UserHardware hardware) {
        DiscordUserHardware discordUserHardware = (DiscordUserHardware) hardware;
        discordUserHardware.banned = false;
        try (Connection connection = mySQLHolder.getConnection()) {
            PreparedStatement s = connection.prepareStatement(sqlUpdateHardwareBanned);
            s.setBoolean(1, false);
            s.setLong(2, discordUserHardware.id);
            s.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL error", e);
        }
    }

    public static class DiscordUser implements User {
        public String username;
        public String discordId;
        public UUID uuid;
        public ClientPermissions permissions;
        public String serverId;
        public String accessToken;
        public String refreshToken;
        public Long expiresIn;
        public Date bannedAt;
        protected Long hwidId;

        public DiscordUser(String username, UUID uuid, String accessToken, String refreshToken, Long expiresIn, String discordId, Date bannedAt, String serverId, Long hwidId) {
            this.username = username;
            this.uuid = uuid;
            this.discordId = discordId;
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
            this.bannedAt = bannedAt;
            this.refreshToken = refreshToken;
            this.permissions = new ClientPermissions();
            this.serverId = serverId;
            this.hwidId = hwidId;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public UUID getUUID() {
            return uuid;
        }

        @Override
        public String getServerId() {
            return serverId;
        }

        @Override
        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public String getDiscordId() {
            return discordId;
        }

        @Override
        public ClientPermissions getPermissions() {
            return permissions;
        }

        @Override
        public boolean isBanned() {
            return this.bannedAt != null;
        }
    }

    public static class DiscordUserSession implements UserSession {
        private final String id;
        public transient DiscordUser user;
        public String accessToken;
        public long expireMillis;

        public DiscordUserSession(DiscordUser user, String accessToken) {
            this.id = SecurityHelper.randomStringToken();
            this.user = user;
            this.accessToken = accessToken;
        }

        @Override
        public String getID() {
            return id;
        }

        @Override
        public User getUser() {
            return user;
        }

        @Override
        public long getExpireIn() {
            return expireMillis;
        }
    }

    public static class DiscordUserHardware implements UserHardware {

        private final HardwareReportRequest.HardwareInfo hardwareInfo;
        private final long id;
        private byte[] publicKey;
        private boolean banned;

        public DiscordUserHardware(HardwareReportRequest.HardwareInfo hardwareInfo, byte[] publicKey, long id, boolean banned) {
            this.hardwareInfo = hardwareInfo;
            this.publicKey = publicKey;
            this.id = id;
            this.banned = banned;
        }

        @Override
        public HardwareReportRequest.HardwareInfo getHardwareInfo() {
            return hardwareInfo;
        }

        @Override
        public byte[] getPublicKey() {
            return publicKey;
        }

        @Override
        public String getId() {
            return String.valueOf(id);
        }

        @Override
        public boolean isBanned() {
            return banned;
        }

        @Override
        public String toString() {
            return "DiscordUserHardware{" +
                    "hardwareInfo=" + hardwareInfo +
                    ", publicKey=" + (publicKey == null ? null : new String(Base64.getEncoder().encode(publicKey))) +
                    ", id=" + id +
                    ", banned=" + banned +
                    '}';
        }
    }
}
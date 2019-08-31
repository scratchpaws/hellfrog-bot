package hellfrog.settings.db;

import hellfrog.common.CommonUtils;
import hellfrog.common.FromTextFile;
import hellfrog.common.ResourcesLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Настройки, индивидуальные для сервера
 * <p>
 * CREATE TABLE "server_preferences" (
 * "id"	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
 * "server_id"	INTEGER NOT NULL,
 * "key"	TEXT NOT NULL,
 * "value"	TEXT,
 * "create_date"	INTEGER NOT NULL DEFAULT 0,
 * "update_date"	INTEGER NOT NULL DEFAULT 0,
 * CONSTRAINT "uniq_serv_key" UNIQUE ("server_id","key")
 * );
 * </p>
 */
public class ServerPreferencesDAO {

    private static final String TABLE_NAME = "server_preferences";
    private static final String PREFIX_KEY = "bot.prefix";
    static final String PREFIX_DEFAULT = ">>";
    private static final String JOIN_LEAVE_DISPLAY_KEY = "join.leave.key";
    static final boolean JOIN_LEAVE_DISPLAY_DEFAULT = false;
    private static final String JOIN_LEAVE_CHANNEL_ID_KEY = "join.leave.channel";
    static final long JOIN_LEAVE_CHANNEL_ID_DEFAULT = 0L;
    private static final String NEW_ACL_MODE_KEY = "new.acl.mode";
    static final boolean NEW_ACL_MODE_DEFAULT = true;
    private static final String AUTO_PROMOTE_ENABLED_KEY = "auto.promote.enabled";
    static final boolean AUTO_PROMOTE_ENABLED_DEFAULT = false;
    private static final String AUTO_PROMOTE_ROLE_ID_KEY = "auto.promote.role";
    static final long AUTO_PROMOTE_ROLE_ID_DEFAULT = 0L;
    private static final String AUTO_PROMOTE_TIMEOUT_KEY = "auto.promote.timeout";
    static final long AUTO_PROMOTE_TIMEOUT_DEFAULT = 0L;
    private static final String COMMUNITY_CONTROL_THRESHOLD_KEY = "com.control.threshold";
    static final long COMMUNITY_CONTROL_THRESHOLD_DEFAULT = 0L;
    private static final String COMMUNITY_CONTROL_ROLE_ID_KEY = "com.control.role";
    static final long COMMUNITY_CONTROL_ROLE_ID_DEFAULT = 0L;
    private static final String COMMUNITY_CONTROL_CUSTOM_EMOJI_ID_KEY = "com.control.emoji.id";
    static final long COMMUNITY_CONTROL_CUSTOM_EMOJI_ID_DEFAULT = 0L;
    private static final String COMMUNITY_CONTROL_UNICODE_EMOJI_KEY = "com.control.unicode.emoji";
    static final String COMMUNITY_CONTROL_UNICODE_EMOJI_DEFAULT = "";

    private final Logger log = LogManager.getLogger("Server preferences");
    private final Connection connection;

    @FromTextFile(fileName = "sql/server_preferences/get_value_query.sql")
    private String getValueQuery = null;
    @FromTextFile(fileName = "sql/server_preferences/insert_value_query.sql")
    private String insertQuery = null;
    @FromTextFile(fileName = "sql/server_preferences/update_value_query.sql")
    private String updateQuery = null;

    ServerPreferencesDAO(@NotNull Connection connection) {
        this.connection = connection;
        ResourcesLoader.initFileResources(this, ServerPreferencesDAO.class);
    }

    private Optional<String> upsertString(long serverId,
                                          @NotNull final String key,
                                          @NotNull final String value,
                                          boolean override) {

        String currentValue = null;
        boolean present = false;

        try (PreparedStatement statement = connection.prepareStatement(getValueQuery)) {
            statement.setLong(1, serverId);
            statement.setString(2, key);
            if (log.isDebugEnabled()) {
                log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}", getValueQuery, serverId, key);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    currentValue = resultSet.getString(1);
                    present = true;
                    if (log.isDebugEnabled()) {
                        log.debug("Value for key \"{}\" present, is \"{}\"", key, currentValue);
                    }
                }
            }
        } catch (SQLException err) {
            log.error("Unable to get value from \"" + TABLE_NAME + "\": " + err.getMessage(), err);
        }

        if (!present) {
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                statement.setLong(1, serverId);
                statement.setString(2, key);
                statement.setString(3, value);
                long createDate = CommonUtils.getCurrentGmtTimeAsMillis();
                statement.setLong(4, createDate);
                statement.setLong(5, createDate);
                if (log.isDebugEnabled()) {
                    log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4,5: {}",
                            insertQuery, serverId, key, value, createDate);
                }
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("\nInserted {} values", count);
                }
                return Optional.empty();
            } catch (SQLException fail) {
                String errMsg = String.format("Unable to insert for server %d and key \"%s\" value \"%s\": %s",
                        serverId, key, value, fail.getMessage());
                log.error(errMsg, fail);
            }
        }

        if (override) {
            try (PreparedStatement statement = connection.prepareStatement(updateQuery)) {
                statement.setString(1, value);
                long updateDate = CommonUtils.getCurrentGmtTimeAsMillis();
                statement.setLong(2, updateDate);
                statement.setLong(3, serverId);
                statement.setString(4, key);
                if (log.isDebugEnabled()) {
                    log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4: {}",
                            updateQuery, value, updateDate, serverId, key);
                }
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Updated {} values", count);
                }
            } catch (SQLException fail) {
                String errMsg = String.format("Unable to update for server %d and key \"%s\" value \"%s\": %s",
                        serverId, key, value, fail.getMessage());
                log.error(errMsg, fail);
            }
        }
        return Optional.ofNullable(currentValue);
    }

    private Optional<Long> upsertLong(long serverId, @NotNull final String key, long value, boolean override) {
        return upsertString(serverId, key, String.valueOf(value), override)
                .map(CommonUtils::onlyNumbersToLong);
    }

    private Optional<Boolean> upsertBoolean(long serverId, @NotNull final String key, boolean value, boolean override) {
        return upsertString(serverId, key, (value ? "1" : "0"), override)
                .map(result -> result.equals("1"));
    }

    @Contract("_, _, !null -> !null")
    private String getStringValue(long serverId, @NotNull String key, @NotNull String defaultValue) {
        return upsertString(serverId, key, defaultValue, false)
                .orElse(defaultValue);
    }

    @Contract("_, _, _, !null -> !null")
    private String setStringValue(long serverId, @NotNull String key, @NotNull String newValue, @NotNull String defaultValue) {
        return upsertString(serverId, key, newValue, true)
                .orElse(defaultValue);
    }

    private boolean getBooleanValue(long serverId, @NotNull String key, boolean defaultValue) {
        return upsertBoolean(serverId, key, defaultValue, false)
                .orElse(defaultValue);
    }

    private boolean setBooleanValue(long serverId, @NotNull String key, boolean newValue, boolean defaultValue) {
        return upsertBoolean(serverId, key, newValue, true)
                .orElse(defaultValue);
    }

    private long getLongValue(long serverId, @NotNull String key, long defaultValue) {
        return upsertLong(serverId, key, defaultValue, false)
                .orElse(defaultValue);
    }

    private long setLongValue(long serverId, @NotNull String key, long newValue, long defaultValue) {
        return upsertLong(serverId, key, newValue, true)
                .orElse(defaultValue);
    }

    public String getPrefix(long serverId) {
        return getStringValue(serverId, PREFIX_KEY, PREFIX_DEFAULT);
    }

    public String setPrefix(long serverId, @NotNull String newPrefix) {
        return setStringValue(serverId, PREFIX_KEY, newPrefix, PREFIX_DEFAULT);
    }

    public boolean isJoinLeaveDisplay(long serverId) {
        return getBooleanValue(serverId, JOIN_LEAVE_DISPLAY_KEY, JOIN_LEAVE_DISPLAY_DEFAULT);
    }

    public boolean setJoinLeaveDisplay(long serverId, boolean newState) {
        return setBooleanValue(serverId, JOIN_LEAVE_DISPLAY_KEY, newState, JOIN_LEAVE_DISPLAY_DEFAULT);
    }

    public long getJoinLeaveChannel(long serverId) {
        return getLongValue(serverId, JOIN_LEAVE_CHANNEL_ID_KEY, JOIN_LEAVE_CHANNEL_ID_DEFAULT);
    }

    public long setJoinLeaveChannel(long serverId, long newChannelId) {
        return setLongValue(serverId, JOIN_LEAVE_CHANNEL_ID_KEY, newChannelId, JOIN_LEAVE_CHANNEL_ID_DEFAULT);
    }

    public boolean isNewAclMode(long serverId) {
        return getBooleanValue(serverId, NEW_ACL_MODE_KEY, NEW_ACL_MODE_DEFAULT);
    }

    public boolean setNewAclMode(long serverId, boolean isNewMode) {
        return setBooleanValue(serverId, NEW_ACL_MODE_KEY, isNewMode, NEW_ACL_MODE_DEFAULT);
    }

}

package hellfrog.settings.db.sqlite;

import hellfrog.common.CommonUtils;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.db.CommonPreferencesDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Менеджер основных параметров
 * <p>
 * CREATE TABLE "common_preferences" (
 * "key"	TEXT NOT NULL UNIQUE,
 * "value"	TEXT NOT NULL,
 * "create_date"	INTEGER NOT NULL DEFAULT 0,
 * "update_date"	INTEGER NOT NULL DEFAULT 0,
 * PRIMARY KEY("key")
 * )
 */
class CommonPreferencesDAOImpl
        implements CommonPreferencesDAO {

    private static final String TABLE_NAME = "common_preferences";

    private final Connection connection;
    private final Logger log = LogManager.getLogger("Common preferences");

    private final String getValueQuery = ResourcesLoader.fromTextFile("sql/common_preferences/get_value_query.sql");
    private final String insertQuery = ResourcesLoader.fromTextFile("sql/common_preferences/insert_value_query.sql");
    private final String updateQuery = ResourcesLoader.fromTextFile("sql/common_preferences/update_value_query.sql");

    CommonPreferencesDAOImpl(@NotNull Connection connection) {
        this.connection = connection;
    }

    private Optional<String> upsert(@NotNull final String key,
                                    @NotNull final String value,
                                    final boolean override) {

        String currentValue = null;
        boolean present = false;

        try (PreparedStatement statement = connection.prepareStatement(getValueQuery)) {
            statement.setString(1, key);
            if (log.isDebugEnabled()) {
                log.debug("Query:\n{}\nParam 1: {}", getValueQuery, key);
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
                statement.setString(1, key);
                statement.setString(2, value);
                long createDate = CommonUtils.getCurrentGmtTimeAsMillis();
                statement.setLong(3, createDate);
                statement.setLong(4, createDate);
                if (log.isDebugEnabled()) {
                    log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3, 4: {}",
                            insertQuery, key, value, createDate);
                }
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("\nInserted {} values", count);
                }
                return Optional.empty();
            } catch (SQLException fail) {
                log.error("Unable to insert \"" + key + "\" - \"" + value + "\" to \"" + TABLE_NAME
                        + "\": " + fail.getMessage(), fail);
            }
        }

        if (override) {
            try (PreparedStatement statement = connection.prepareStatement(updateQuery)) {
                statement.setString(1, value);
                long updateDate = CommonUtils.getCurrentGmtTimeAsMillis();
                statement.setLong(2, updateDate);
                statement.setString(3, key);
                if (log.isDebugEnabled()) {
                    log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}",
                            updateQuery, value, updateDate, key);
                }
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Updated {} values", count);
                }
            } catch (SQLException fail) {
                log.error("Unable to update \"" + key + "\" - \"" + value + "\" into \"" + TABLE_NAME
                        + "\": " + fail.getMessage(), fail);
            }
        }
        return Optional.ofNullable(currentValue);
    }

    public String getApiKey() {
        return upsert(API_KEY, API_KEY_DEFAULT, false).orElse(API_KEY_DEFAULT);
    }

    public String setApiKey(@NotNull String newApiKey) {
        return upsert(API_KEY, newApiKey, true).orElse(API_KEY_DEFAULT);
    }

    public String getBotPrefix() {
        return upsert(PREFIX_KEY, PREFIX_DEFAULT, false).orElse(PREFIX_DEFAULT);
    }

    public String setBotPrefix(@NotNull String newBotPrefix) {
        return upsert(PREFIX_KEY, newBotPrefix, true).orElse(PREFIX_DEFAULT);
    }

    public String getBotName() {
        return upsert(BOT_NAME_KEY, BOT_NAME_DEFAULT, false).orElse(BOT_NAME_DEFAULT);
    }

    public String setBotName(@NotNull String newBotName) {
        return upsert(BOT_NAME_KEY, newBotName, true).orElse(BOT_NAME_DEFAULT);
    }

    public long getOfficialBotServerId() {
        try {
            return Long.parseLong(upsert(OFFICIAL_SERVER_KEY, OFFICIAL_SERVER_DEFAULT_STR, false)
                    .orElse(OFFICIAL_SERVER_DEFAULT_STR));
        } catch (NumberFormatException | NullPointerException err) {
            log.error("Unable to parse official bot server id value: {}", err.getMessage());
            return OFFICIAL_SERVER_DEFAULT;
        }
    }

    public long setOfficialBotServerId(long newServerId) {
        try {
            return Long.parseLong(upsert(OFFICIAL_SERVER_KEY, String.valueOf(newServerId), true)
                    .orElse(OFFICIAL_SERVER_DEFAULT_STR));
        } catch (NumberFormatException | NullPointerException err) {
            log.error("Unable to parse official bot server id value: {}", err.getMessage());
            return OFFICIAL_SERVER_DEFAULT;
        }
    }

    public long getBotServiceChannelId() {
        try {
            return Long.parseLong(upsert(SERVICE_CHANNEL_KEY, SERVICE_CHANNEL_DEFAULT_STR, false)
                    .orElse(SERVICE_CHANNEL_DEFAULT_STR));
        } catch (NumberFormatException | NullPointerException err) {
            log.error("Unable to parse service channel id value: {}", err.getMessage());
            return SERVICE_CHANNEL_DEFAULT;
        }
    }

    public long setBotServiceChannelId(long newBotServiceChannelId) {
        try {
            return Long.parseLong(upsert(SERVICE_CHANNEL_KEY, String.valueOf(newBotServiceChannelId), true)
                    .orElse(SERVICE_CHANNEL_DEFAULT_STR));
        } catch (NumberFormatException | NullPointerException err) {
            log.error("Unable to parse service channel id value: {}", err.getMessage());
            return SERVICE_CHANNEL_DEFAULT;
        }
    }

    public long getHighRollChannelId() {
        try {
            return Long.parseLong(upsert(HIGH_ROLL_CHANNEL_KEY, HIGH_ROLL_CHANNEL_KEY_DEFAULT_STR, false)
                    .orElse(HIGH_ROLL_CHANNEL_KEY_DEFAULT_STR));
        } catch (NullPointerException | NumberFormatException err) {
            log.error("Unable to parse channel id with images for high rolls: {}", err.getMessage());
            return HIGH_ROLL_CHANNEL_KEY_DEFAULT;
        }
    }

    public long setHighRollChannelId(long newHighRollChannelId) {
        try {
            return Long.parseLong(upsert(HIGH_ROLL_CHANNEL_KEY, String.valueOf(newHighRollChannelId), true)
                    .orElse(HIGH_ROLL_CHANNEL_KEY_DEFAULT_STR));
        } catch (NullPointerException | NumberFormatException err) {
            log.error("Unable to parse channel id with images for high rolls: {}", err.getMessage());
            return HIGH_ROLL_CHANNEL_KEY_DEFAULT;
        }
    }

    public long getLowRollChannelId() {
        try {
            return Long.parseLong(upsert(LOW_ROLL_CHANNEL_KEY, LOW_ROLL_CHANNEL_DEFAULT_STR, false)
                    .orElse(LOW_ROLL_CHANNEL_DEFAULT_STR));
        } catch (NumberFormatException | NullPointerException err) {
            log.error("Unable to parse channel id with images for low rolls: {}", err.getMessage());
            return LOW_ROLL_CHANNEL_DEFAULT;
        }
    }

    public long setLowRollChannelId(long newLowRollChannelId) {
        try {
            return Long.parseLong(upsert(LOW_ROLL_CHANNEL_KEY, String.valueOf(newLowRollChannelId), true)
                    .orElse(LOW_ROLL_CHANNEL_DEFAULT_STR));
        } catch (NumberFormatException | NullPointerException err) {
            log.error("Unable to parse channel id with images for low rolls: {}", err.getMessage());
            return LOW_ROLL_CHANNEL_DEFAULT;
        }
    }
}

package hellfrog.settings.db;

import hellfrog.common.CommonUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Менеджер основных параметров
 *
 * CREATE TABLE "common_preferences" (
 * 	"key"	TEXT NOT NULL UNIQUE,
 * 	"value"	TEXT NOT NULL,
 * 	"create_date"	INTEGER NOT NULL DEFAULT 0,
 * 	"update_date"	INTEGER NOT NULL DEFAULT 0,
 * 	PRIMARY KEY("key")
 * )
 */
public class DBCommonPreferences {

    private static final String TABLE_NAME = "common_preferences";
    private static final String GET_VALUE_QUERY = "select \"value\" from common_preferences where \"key\" = ?";
    private static final String INSERT_QUERY = "insert into common_preferences\n" +
            "(\"key\", \"value\", \"create_date\", \"update_date\")\n" +
            "values\n" +
            "(? , ?, ?, ?)";
    private static final String UPDATE_QUERY = "update common_preferences\n" +
            "set \"value\" = ?, update_date = ?\n" +
            "where \"key\" = ?";
    private static final String PREFIX_KEY = "bot.prefix";
    private static final String PREFIX_DEFAULT = ">>";
    private static final String API_KEY = "api.key";
    private static final String API_KEY_DEFAULT = "";
    private static final String BOT_NAME_KEY = "bot.name";
    private static final String BOT_NAME_DEFAULT = "HellFrog";

    private final Connection connection;
    private final Logger log = LogManager.getLogger("Common preferences");

    public DBCommonPreferences(@NotNull Connection connection) {
        this.connection = connection;
    }

    @NotNull
    private ImmutablePair<Boolean, String> getRawValueFromTable(@NotNull String key) {
        if (CommonUtils.isTrStringNotEmpty(key)) {
            try (PreparedStatement statement = connection.prepareStatement(GET_VALUE_QUERY)) {
                statement.setString(1, key);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String value = resultSet.getString(1);
                        return ImmutablePair.of(Boolean.TRUE, value);
                    }
                }
            } catch (SQLException err) {
                log.error("Unable to get value from \"" + TABLE_NAME + "\": " + err.getMessage(), err);
            }
        }
        return ImmutablePair.of(Boolean.FALSE, null);
    }

    private void writeRawValueToTable(@NotNull String key, @NotNull String value) {
        ImmutablePair<Boolean, String> currentValue = getRawValueFromTable(key);
        if (currentValue.left == Boolean.FALSE) {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_QUERY)) {
                statement.setString(1, key);
                statement.setString(2, value);
                Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
                long createDate = calendar.getTimeInMillis();
                statement.setLong(3, createDate);
                statement.setLong(4, createDate);
                statement.executeUpdate();
            } catch (SQLException fail) {
                log.error("Unable to insert \"" + key + "\" - \"" + value + "\" to \"" + TABLE_NAME
                        + "\": " + fail.getMessage(), fail);
            }
        } else {
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_QUERY)) {
                statement.setString(1, value);
                Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
                long updateDate = calendar.getTimeInMillis();
                statement.setLong(2, updateDate);
                statement.setString(3, key);
                statement.executeUpdate();
            } catch (SQLException fail) {
                log.error("Unable to update \"" + key + "\" - \"" + value + "\" into \"" + TABLE_NAME
                        + "\": " + fail.getMessage(), fail);
            }
        }
    }

    private String getStringValue(@Nullable String key, @Nullable String defaultValue) {
        if (CommonUtils.isTrStringEmpty(key) || CommonUtils.isTrStringEmpty(defaultValue)) {
            return defaultValue;
        }
        ImmutablePair<Boolean, String> currentValue = getRawValueFromTable(key);
        if (currentValue.left == Boolean.FALSE) {
            writeRawValueToTable(key, defaultValue);
            return defaultValue;
        } else {
            return currentValue.right;
        }
    }

    public String getApiKey() {
        return getStringValue(API_KEY, API_KEY_DEFAULT);
    }

    public String setApiKey(@NotNull String newApiKey) {
        String previous = getApiKey();
        writeRawValueToTable(API_KEY, newApiKey);
        return previous;
    }

    public String getBotPrefix() {
        return getStringValue(PREFIX_KEY, PREFIX_DEFAULT);
    }

    public String setBotPrefix(@NotNull String newBotPrefix) {
        String previous = getBotPrefix();
        writeRawValueToTable(PREFIX_KEY, newBotPrefix);
        return previous;
    }

    public String getBotName() {
        return getStringValue(BOT_NAME_KEY, BOT_NAME_DEFAULT);
    }

    public String setBotName(@NotNull String newBotName) {
        String previous = getBotName();
        writeRawValueToTable(BOT_NAME_KEY, newBotName);
        return previous;
    }
}

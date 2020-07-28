package hellfrog.settings.db.sqlite;

import hellfrog.common.CommonUtils;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.db.BotOwnersDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DAO владельцев бота (не считая неотзывного владельца API Key)
 * <p>
 * CREATE TABLE "bot_owners" (
 * "user_id"	    INTEGER NOT NULL UNIQUE,
 * "create_date"	INTEGER NOT NULL DEFAULT 0,
 * PRIMARY KEY("user_id")
 * );
 * </p>
 */
class BotOwnersDAOImpl
        implements BotOwnersDAO {

    private static final String GET_ALL = "select user_id from bot_owners";
    private static final String CHECK_EXISTS = "select count(1) from bot_owners where user_id = ?";
    private static final String DELETE_VALUE = "delete from bot_owners where user_id = ?";
    private static final String TABLE_NAME = "bot_owners";

    private final Connection connection;
    private final Logger log = LogManager.getLogger("Bot owners");

    private final String insertValueQuery = ResourcesLoader.fromTextFile("sql/bot_owners/insert_value_query.sql");

    BotOwnersDAOImpl(@NotNull Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Long> getAll() {
        List<Long> result = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            if (log.isDebugEnabled()) {
                log.debug("Query:\n{}", GET_ALL);
            }
            try (ResultSet resultSet = statement.executeQuery(GET_ALL)) {
                while (resultSet.next()) {
                    result.add(resultSet.getLong(1));
                }
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to get all bot owners from \"%s\" table: %s",
                    TABLE_NAME, err.getMessage());
            log.error(errMsg, err);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean isPresent(long userId) {
        try (PreparedStatement statement = connection.prepareStatement(CHECK_EXISTS)) {
            statement.setLong(1, userId);
            if (log.isDebugEnabled()) {
                log.debug("Query:\n{}\nParam 1: {}", CHECK_EXISTS, userId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    if (log.isDebugEnabled()) {
                        log.debug("Found {} values for user id {} into bot_owners", count, userId);
                    }
                    return count > 0;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("No values found for user id {} into bot_owners", userId);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to check what user with id %d is present into table \"%s\": %s",
                    userId, TABLE_NAME, err.getMessage());
            log.error(errMsg, err);
        }
        return false;
    }

    @Override
    public boolean addToOwners(long userId) {
        if (!isPresent(userId)) {
            try (PreparedStatement statement = connection.prepareStatement(insertValueQuery)) {
                statement.setLong(1, userId);
                long createDate = CommonUtils.getCurrentGmtTimeAsMillis();
                statement.setLong(2, createDate);
                if (log.isDebugEnabled()) {
                    log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}", insertValueQuery, userId, createDate);
                }
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Inserted {} values", count);
                }
                return count == 1;
            } catch (SQLException err) {
                String errMsg = String.format("Unable to add %d to global bot owners: %s", userId, err.getMessage());
                log.error(errMsg, err);
            }
        }
        return false;
    }

    @Override
    public boolean deleteFromOwners(long userId) {
        if (isPresent(userId)) {
            try (PreparedStatement statement = connection.prepareStatement(DELETE_VALUE)) {
                statement.setLong(1, userId);
                if (log.isDebugEnabled()) {
                    log.debug("Query:\n{}\nParam 1: {}", DELETE_VALUE, userId);
                }
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Deleted {} values", count);
                }
                return count > 0;
            } catch (SQLException err) {
                String errMsg = String.format("Unable to delete %d from global bot owners: %s", userId, err.getMessage());
                log.error(errMsg, err);
            }
        }
        return false;
    }
}

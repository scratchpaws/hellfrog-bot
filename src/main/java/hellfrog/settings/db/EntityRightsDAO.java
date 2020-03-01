package hellfrog.settings.db;

import hellfrog.common.CommonUtils;
import hellfrog.common.FromTextFile;
import hellfrog.common.ResourcesLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DAO прав пользователя, роли, либо канала/категории.
 * Таблицы для данных сущностей однотипны и вынесены в данный класс
 * <p>
 * CREATE TABLE        "entity_name_rights" (<br>
 * "id"                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,<br>
 * "server_id"         INTEGER NOT NULL,<br>
 * "command_prefix"    TEXT NOT NULL,<br>
 * "entity_name_id"    INTEGER NOT NULL,<br>
 * "create_date"       INTEGER NOT NULL DEFAULT 0,<br>
 * );<br>
 * </p>
 */
public abstract class EntityRightsDAO {

    private final Logger log;
    private final Connection connection;

    private final String columnName;
    private final String tableName;

    private static final String TABLE_NAME_TEMPLATE = "%table_name%";
    private static final String COLUMN_NAME_TEMPLATE = "%column_name%";

    @FromTextFile(fileName = "sql/entity_rights/get_all_for_server_query.sql")
    private String getAllAllowedOnServerQuery = null;
    @FromTextFile(fileName = "sql/entity_rights/check_is_allowed_on_server_query.sql")
    private String checkIsAllowedOnServerQuery = null;
    @FromTextFile(fileName = "sql/entity_rights/allow_on_server_query.sql")
    private String allowOnServerQuery = null;
    @FromTextFile(fileName = "sql/entity_rights/deny_on_server_query.sql")
    private String denyOnServerQuery = null;

    public EntityRightsDAO(@NotNull Connection connection,
                           @NotNull String tableName,
                           @NotNull String columnName) {
        this.connection = connection;
        this.columnName = columnName;
        this.tableName = tableName;
        this.log = LogManager.getLogger("Entity rights (" + tableName + ")");
        ResourcesLoader.initFileResources(this, EntityRightsDAO.class);
        getAllAllowedOnServerQuery = getAllAllowedOnServerQuery.replace(TABLE_NAME_TEMPLATE, tableName)
                .replace(COLUMN_NAME_TEMPLATE, columnName);
        checkIsAllowedOnServerQuery = checkIsAllowedOnServerQuery.replace(TABLE_NAME_TEMPLATE, tableName)
                .replace(COLUMN_NAME_TEMPLATE, columnName);
        allowOnServerQuery = allowOnServerQuery.replace(TABLE_NAME_TEMPLATE, tableName)
                .replace(COLUMN_NAME_TEMPLATE, columnName);
        denyOnServerQuery = denyOnServerQuery.replace(TABLE_NAME_TEMPLATE, tableName)
                .replace(COLUMN_NAME_TEMPLATE, columnName);
    }

    public List<Long> getAllAllowed(long serverId, @NotNull String commandPrefix) {
        List<Long> result = new ArrayList<>();
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}", getAllAllowedOnServerQuery,
                    serverId, commandPrefix);
        }
        try (PreparedStatement statement = connection.prepareStatement(getAllAllowedOnServerQuery)) {
            statement.setLong(1, serverId);
            statement.setString(2, commandPrefix);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(resultSet.getLong(1));
                }
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to get all allowed %s for command \"%s\" " +
                            "and server id \"%d\" from table \"%s\": %s", columnName, commandPrefix,
                    serverId, tableName, err.getMessage());
            log.error(errMsg, err);
        }
        return Collections.unmodifiableList(result);
    }

    public boolean isAllowed(long serverId, long who, @NotNull String commandPrefix) {
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}",
                    checkIsAllowedOnServerQuery, serverId, who, commandPrefix);
        }
        try (PreparedStatement statement = connection.prepareStatement(checkIsAllowedOnServerQuery)) {
            statement.setLong(1, serverId);
            statement.setLong(2, who);
            statement.setString(3, commandPrefix);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int result = resultSet.getInt(1);
                    if (log.isDebugEnabled()) {
                        log.debug("Found {} values for serverId {}, entityId {}, commandPrefix {}, " +
                                "table name {}", result, serverId, who, commandPrefix, tableName);
                    }
                    return result > 0;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("No values found for serverId {}, entityId {}, commandPrefix {}, table name {}"
                        , serverId, who, commandPrefix, tableName);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to check what %s with id \"%d\" is granted " +
                            "to execute command \"%s\" on server with id \"%d\" (table \"%s\"): %s",
                    columnName, who, commandPrefix, serverId, tableName, err.getMessage());
            log.error(errMsg, err);
        }

        return false;
    }

    public boolean allow(long serverId, long who, @NotNull String commandPrefix) {
        if (!isAllowed(serverId, who, commandPrefix)) {
            long createDate = CommonUtils.getCurrentGmtTimeAsMillis();
            if (log.isDebugEnabled()) {
                log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4: {}",
                        allowOnServerQuery, serverId, commandPrefix, who, createDate);
            }
            try (PreparedStatement statement = connection.prepareStatement(allowOnServerQuery)) {
                statement.setLong(1, serverId);
                statement.setString(2, commandPrefix);
                statement.setLong(3, who);
                statement.setLong(4, createDate);
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Inserted {} values", count);
                }
                return count == 1;
            } catch (SQLException err) {
                String errMsg = String.format("Unable to allow %s with id \"%d\" execute command " +
                                "\"%s\" on server with id \"%d\" (table \"%s\"): %s",
                        columnName, who, commandPrefix, serverId, tableName, err.getMessage());
                log.error(errMsg, err);
                return false;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Entity {} with id {} already allowed to execute command " +
                                "with prefix {} on serverId {}, table {}",
                        columnName, who, commandPrefix, serverId, tableName);
            }
            return false;
        }
    }

    public boolean deny(long serverId, long who, @NotNull String commandPrefix) {
        if (isAllowed(serverId, who, commandPrefix)) {
            if (log.isDebugEnabled()) {
                log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}",
                        denyOnServerQuery, serverId, who, commandPrefix);
            }
            try (PreparedStatement statement = connection.prepareStatement(denyOnServerQuery)) {
                statement.setLong(1, serverId);
                statement.setLong(2, who);
                statement.setString(3, commandPrefix);
                int count = statement.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("Deleted {} values", count);
                }
                return count > 0;
            } catch (SQLException err) {
                String errMsg = String.format("Unable to deny %s with id \"%d\" execute command " +
                                "\"%s\" on server with id \"%d\" (table \"%s\"): %s",
                        columnName, who, commandPrefix, serverId, tableName, err.getMessage());
                log.error(errMsg, err);
                return false;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Entity {} with id {} already denied to execute command " +
                                "with prefix {} on serverId {}, table {}",
                        columnName, who, commandPrefix, serverId, tableName);
            }
            return false;
        }
    }
}

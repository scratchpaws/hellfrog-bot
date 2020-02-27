package hellfrog.settings.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.common.CommonUtils;
import hellfrog.common.FromTextFile;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.entity.RightEntity;
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
public abstract class EntityRightsDAO<T extends RightEntity, ID extends Long>
        extends BaseDaoImpl<T, ID> {

    private final Logger log;
    private Connection connection;

    private String columnName = "";
    private String tableName = "";

    @FromTextFile(fileName = "sql/entity_rights/allow_on_server_query.sql")
    private String allowOnServerQuery = null;
    @FromTextFile(fileName = "sql/entity_rights/deny_on_server_query.sql")
    private String denyOnServerQuery = null;

    public EntityRightsDAO(@NotNull ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
        this.log = LogManager.getLogger("Entity rights (" + dataClass.getSimpleName() + ")");
    }

    public List<Long> getAllAllowed(long serverId, @NotNull String commandPrefix) {
        List<Long> result = new ArrayList<>();
        try {
            List<T> list = super.queryBuilder().where()
                    .eq(RightEntity.SERVER_ID_FIELD_NAME, serverId)
                    .and().eq(RightEntity.COMMAND_PREFIX_FIELD_NAME, commandPrefix)
                    .query();
            for (T entry : list) {
                result.add(entry.getEntityId());
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to get allowed %s for command \"%s\" and server id \"%d\": %s",
                    super.getDataClass().getSimpleName(), commandPrefix, serverId, err.getMessage());
            log.error(errMsg, err);
        }
        if (log.isDebugEnabled()) {
            log.debug("Found allowed entities ids for {}: {}", super.getDataClass().getSimpleName(),
                    result.stream().map(String::valueOf).reduce(CommonUtils::reduceConcat).orElse("[EMPTY]"));
        }
        return Collections.unmodifiableList(result);
    }

    protected abstract String getEntityFieldName();

    public boolean isAllowed(long serverId, long who, @NotNull String commandPrefix) {
        try {
            T entity = super.queryBuilder().where()
                    .eq(RightEntity.SERVER_ID_FIELD_NAME, serverId)
                    .and().eq(RightEntity.COMMAND_PREFIX_FIELD_NAME, commandPrefix)
                    .and().eq(getEntityFieldName(), who)
                    .queryForFirst();
            if (log.isDebugEnabled()) {
                if (entity == null) {
                    log.debug("No values found for serverId {}, entityId {}, commandPrefix {}, entity {}"
                            , serverId, who, commandPrefix, super.getDataClass().getSimpleName());
                } else {
                    log.debug("Found value for serverId {}, entityId {}, commandPrefix {}, " +
                            "entity {}: {}", serverId, who, commandPrefix, super.getDataClass().getSimpleName(), entity.toString());
                }
            }
            return entity != null;
        } catch (SQLException err) {
            String errMsg = String.format("Unable to check what %s with server id \"%d\" and entity id \"%d\" is" +
                            "allow execute command %s: %s", super.getDataClass().getSimpleName(), serverId, who,
                    commandPrefix, err.getMessage());
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

package hellfrog.settings.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.common.CommonUtils;
import hellfrog.settings.entity.RightEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DAO прав пользователя, роли, либо канала/категории.
 * Таблицы для данных сущностей однотипны и вынесены в данный класс
 */
public abstract class EntityRightsDAO<T extends RightEntity, ID extends Long>
        extends BaseDaoImpl<T, ID> {

    private final Logger log;

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

    protected abstract String getEntityFieldIdName();

    protected abstract T createEntity();

    @Nullable
    private T fetchValue(long serverId, long who, @NotNull String commandPrefix) {
        try {
            T entity = super.queryBuilder().where()
                    .eq(RightEntity.SERVER_ID_FIELD_NAME, serverId)
                    .and().eq(RightEntity.COMMAND_PREFIX_FIELD_NAME, commandPrefix)
                    .and().eq(getEntityFieldIdName(), who)
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
            return entity;
        } catch (SQLException err) {
            String errMsg = String.format("Unable to check what %s with server id \"%d\" and entity id \"%d\" is" +
                            "allow execute command %s: %s", super.getDataClass().getSimpleName(), serverId, who,
                    commandPrefix, err.getMessage());
            log.error(errMsg, err);
            return null;
        }
    }

    public boolean isAllowed(long serverId, long who, @NotNull String commandPrefix) {
        return fetchValue(serverId, who, commandPrefix) != null;
    }

    public boolean allow(long serverId, long who, @NotNull String commandPrefix) {
        T entity = fetchValue(serverId, who, commandPrefix);
        if (entity == null) {
            T newEntity = createEntity();
            newEntity.setCommandPrefix(commandPrefix);
            newEntity.setCreateDate(Instant.now());
            newEntity.setServerId(serverId);
            newEntity.setEntityId(who);
            if (log.isDebugEnabled()) {
                log.debug("Storing object {}", newEntity.toString());
            }
            try {
                CreateOrUpdateStatus status = super.createOrUpdate(newEntity);
                if (log.isDebugEnabled()) {
                    log.debug("Update status: created - {}, updated - {}, lines changed - {}",
                            status.isCreated(), status.isUpdated(), status.getNumLinesChanged());
                }
                return status.isCreated() || status.isUpdated();
            } catch (SQLException err) {
                String errMsg = String.format("Unable to persist %s: %s", newEntity, err.getMessage());
                log.error(errMsg, err);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Entity {} with id {} already allowed to execute command " +
                                "with prefix {} on serverId {}: {}",
                        super.getDataClass().getSimpleName(), who, commandPrefix, serverId, entity);
            }
        }
        return false;
    }

    public boolean deny(long serverId, long who, @NotNull String commandPrefix) {
        T entity = fetchValue(serverId, who, commandPrefix);
        if (entity != null) {
            if (log.isDebugEnabled()) {
                log.debug("Deleting {}", entity.toString());
            }
            try {
                DeleteBuilder<T, ID> deleteBuilder = super.deleteBuilder();
                deleteBuilder.where()
                        .eq(RightEntity.COMMAND_PREFIX_FIELD_NAME, commandPrefix)
                        .and().eq(RightEntity.SERVER_ID_FIELD_NAME, serverId)
                        .and().eq(getEntityFieldIdName(), who);
                int count = deleteBuilder.delete();
                if (log.isDebugEnabled()) {
                    log.debug("Deleted {} entities with server id {}, entity id {}, command prefix {}",
                            count, serverId, who, commandPrefix);
                }
                return count > 0;
            } catch (SQLException err) {
                String errMsg = String.format("Unable to deny %s with id \"%d\" execute command " +
                                "\"%s\" on server with id \"%d\": %s",
                        super.getDataClass().getSimpleName(), who, commandPrefix, serverId, err.getMessage());
                log.error(errMsg, err);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Entity {} with id {} already denied to execute command " +
                                "with prefix {} on serverId {}",
                        super.getDataClass().getSimpleName(), who, commandPrefix, serverId);
            }
        }
        return false;
    }
}

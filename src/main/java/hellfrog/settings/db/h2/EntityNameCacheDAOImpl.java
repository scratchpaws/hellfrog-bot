package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import hellfrog.settings.db.EntityNameCacheDAO;
import hellfrog.settings.db.entity.EntityNameCache;
import hellfrog.settings.db.entity.NameType;
import hellfrog.settings.db.entity.ServerNameCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

class EntityNameCacheDAOImpl
        implements EntityNameCacheDAO {

    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Entity cache");

    private static final String FIND_SERVER_NAME_QUERY = "from " + ServerNameCache.class.getSimpleName() + " sc "
            + " where sc.serverId = :serverId and sc.entityId = :entityId";

    EntityNameCacheDAOImpl(@NotNull AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<EntityNameCache> find(long entityId) {
        try (AutoSession session = sessionFactory.openSession()) {
            EntityNameCache cache = session.find(EntityNameCache.class, entityId);
            return Optional.ofNullable(cache);
        } catch (Exception err) {
            String errMsg = String.format("Unable to load entity cache name for id %d: %s",
                    entityId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Optional.empty();
        }
    }

    @Override
    public void update(long entityId, @NotNull String entityName, @NotNull NameType nameType) {
        try (AutoSession session = sessionFactory.openSession()) {
            EntityNameCache stored = session.find(EntityNameCache.class, entityId);
            if (stored == null) {
                stored = new EntityNameCache();
                stored.setEntityId(entityId);
                stored.setCreateDate(Timestamp.from(Instant.now()));
            }
            stored.setName(entityName);
            stored.setEntityType(nameType);
            stored.setUpdateDate(Timestamp.from(Instant.now()));
            session.save(stored);
        } catch (Exception err) {
            String errMsg = String.format("Unable to store entity name to cache, id %d, name \"%s\", type %s: %s",
                    entityId, entityName, nameType, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public void update(@NotNull EntityNameCache entityNameCache) {
        update(entityNameCache.getEntityId(), entityNameCache.getName(), entityNameCache.getEntityType());
    }

    @Override
    public Optional<ServerNameCache> find(long serverId, long entityId) {
        try (AutoSession session = sessionFactory.openSession()) {
            return session.createQuery(FIND_SERVER_NAME_QUERY, ServerNameCache.class)
                    .setParameter("serverId", serverId)
                    .setParameter("entityId", entityId)
                    .uniqueResultOptional();
        } catch (Exception err) {
            String errMsg = String.format("Unable to load entity cache name for server id %d and entity id %d: %s",
                    serverId, entityId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Optional.empty();
        }
    }

    @Override
    public void update(long serverId, long entityId, @NotNull final String entityName) {
        try (AutoSession session = sessionFactory.openSession()) {
            ServerNameCache nameCache = session.createQuery(FIND_SERVER_NAME_QUERY, ServerNameCache.class)
                    .setParameter("serverId", serverId)
                    .setParameter("entityId", entityId)
                    .uniqueResult();
            if (nameCache == null) {
                nameCache = new ServerNameCache();
                nameCache.setServerId(serverId);
                nameCache.setEntityId(entityId);
                nameCache.setCreateDate(Timestamp.from(Instant.now()));
            }
            nameCache.setName(entityName);
            nameCache.setUpdateDate(Timestamp.from(Instant.now()));
            session.save(nameCache);
        } catch (Exception err) {
            String errMsg = String.format("Unable to store entity name to cache, server id %d, entity id %d, name \"%s\": %s",
                    serverId, entityId, entityName, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    public void update(@NotNull final ServerNameCache nameCache) {
        update(nameCache.getServerId(), nameCache.getEntityId(), nameCache.getName());
    }
}

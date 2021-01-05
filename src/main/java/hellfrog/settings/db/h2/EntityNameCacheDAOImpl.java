package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import hellfrog.settings.db.EntityNameCacheDAO;
import hellfrog.settings.db.entity.EntityNameCache;
import hellfrog.settings.db.entity.NameType;
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
        EntityNameCache nameCache = new EntityNameCache();
        nameCache.setEntityId(entityId);
        nameCache.setName(entityName);
        nameCache.setEntityType(nameType);
        update(nameCache);
    }

    @Override
    public void update(@NotNull EntityNameCache entityNameCache) {
        try (AutoSession session = sessionFactory.openSession()) {
            EntityNameCache stored = session.find(EntityNameCache.class, entityNameCache.getEntityId());
            if (stored == null) {
                stored = entityNameCache;
                stored.setCreateDate(Timestamp.from(Instant.now()));
            } else {
                stored.setName(entityNameCache.getName());
                stored.setEntityType(entityNameCache.getEntityType());
            }
            stored.setUpdateDate(Timestamp.from(Instant.now()));
            session.save(stored);
        } catch (Exception err) {
            String errMsg = String.format("Unable to store entity name to cache \"%s\": %s",
                    entityNameCache.toString(), err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }
}

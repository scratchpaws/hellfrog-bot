package hellfrog.settings.db.h2;

import hellfrog.common.CommonUtils;
import hellfrog.common.TriFunction;
import hellfrog.settings.db.entity.EntityRight;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class EntityRightsDAOImpl<T extends EntityRight> {

    private final AutoSessionFactory sessionFactory;
    private final Logger log;
    private final Class<T> managedClass;
    private final TriFunction<Long, String, Long, T> builder; // server id, command prefix, entity id
    private final String entityField;

    EntityRightsDAOImpl(@NotNull final AutoSessionFactory sessionFactory,
                        @NotNull final String loggerName,
                        @NotNull final Class<T> managedClass,
                        @NotNull final TriFunction<Long, String, Long, T> builder,
                        @NotNull final String entityField) {

        this.sessionFactory = sessionFactory;
        this.log = LogManager.getLogger(loggerName);
        this.managedClass = managedClass;
        this.builder = builder;
        this.entityField = entityField;
    }

    public List<Long> getAllAllowed(long serverId, @NotNull String commandPrefix) {
        if (CommonUtils.isTrStringEmpty(commandPrefix)) {
            return Collections.emptyList();
        }

        try (AutoSession session = sessionFactory.openSession()) {
            List<T> allowed = session.createQuery("from " + managedClass.getSimpleName() + " e where e.serverId = :serverId " +
                    "and e.commandPrefix = :commandPrefix", managedClass)
                    .setParameter("serverId", serverId)
                    .setParameter("commandPrefix", commandPrefix)
                    .list();
            if (allowed != null && !allowed.isEmpty()) {
                List<Long> result = new ArrayList<>(allowed.size());
                for (T item : allowed) {
                    result.add(item.getEntityId());
                }
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to get all allowed \"%s\" for server %d and command \"%s\": %s",
                    managedClass.getSimpleName(), serverId, commandPrefix, err.getMessage());
            log.error(errMsg, err);
        }
        return Collections.emptyList();
    }

    public boolean isAllowed(long serverId, long what, @NotNull String commandPrefix) {
        if (CommonUtils.isTrStringEmpty(commandPrefix)) {
            return false;
        }

        try (AutoSession session = sessionFactory.openSession()) {
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
            Root<T> root = criteriaQuery.from(managedClass);
            criteriaQuery.select(criteriaBuilder.count(root))
                    .where(criteriaBuilder.equal(root.get("serverId"), serverId),
                            criteriaBuilder.equal(root.get("commandPrefix"), commandPrefix),
                            criteriaBuilder.equal(root.get(entityField), what));
            Query<Long> query = session.createQuery(criteriaQuery);
            long result = query.getSingleResult();
            if (log.isDebugEnabled()) {
                log.debug("Found {} values for entity {}, server id {}, entity id {}, command prefix {}",
                        result, managedClass.getSimpleName(), serverId, what, commandPrefix);
            }
            return result > 0L;
        } catch (Exception err) {
            String errMsg = String.format("Unable to check what \"%s\" with id %d is allowed for server %d and command \"%s\": %s",
                    managedClass.getSimpleName(), what, serverId, commandPrefix, err.getMessage());
            log.error(errMsg, err);
        }

        return false;
    }

    public boolean allow(long serverId, long who, @NotNull String commandPrefix) {
        if (!isAllowed(serverId, who, commandPrefix)) {
            try (AutoSession session = sessionFactory.openSession()) {
                T entity = builder.apply(serverId, commandPrefix, who);
                session.save(entity);
                return true;
            } catch (Exception err) {
                String errMsg = String.format("Unable to give access to \"%s\" with id %d to command \"%s\" on server with id %d: %s",
                        managedClass.getSimpleName(), who, commandPrefix, serverId, err.getMessage());
                log.error(errMsg, err);
            }
        } else if (log.isDebugEnabled()) {
            log.debug("Entity {} with id {} already allowed to execute command " +
                    "with prefix {} on serverId {}", managedClass.getSimpleName(), who, commandPrefix, serverId);
        }
        return false;
    }

    public boolean deny(long serverId, long who, @NotNull String commandPrefix) {
        if (isAllowed(serverId, who, commandPrefix)) {
            try (AutoSession session = sessionFactory.openSession()) {
                CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
                CriteriaDelete<T> criteriaDelete = criteriaBuilder.createCriteriaDelete(managedClass);
                Root<T> root = criteriaDelete.from(managedClass);
                criteriaDelete.where(criteriaBuilder.equal(root.get("serverId"), serverId),
                        criteriaBuilder.equal(root.get("commandPrefix"), commandPrefix),
                        criteriaBuilder.equal(root.get(entityField), who));
                Query<?> query = session.createQuery(criteriaDelete);
                int count = query.executeUpdate();
                session.success();
                return count > 0;
            } catch (Exception err) {
                String errMsg = String.format("Unable to deny access to \"%s\" with id %d to command \"%s\" on server with id %d: %s",
                        managedClass.getSimpleName(), who, commandPrefix, serverId, err.getMessage());
                log.error(errMsg, err);
            }
        } else if (log.isDebugEnabled()) {
            log.debug("Entity {} with id {} already denied to execute command " +
                    "with prefix {} on serverId {}", managedClass.getSimpleName(), who, commandPrefix, serverId);
        }
        return false;
    }
}

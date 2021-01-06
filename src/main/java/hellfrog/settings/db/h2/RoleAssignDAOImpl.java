package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import hellfrog.settings.db.RoleAssignDAO;
import hellfrog.settings.db.entity.RoleAssign;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

class RoleAssignDAOImpl
        implements RoleAssignDAO {

    private static final String LOAD_FULL_QUEUE_QUERY = "from " + RoleAssign.class.getSimpleName() + " a "
            + "where a.serverId = :serverId "
            + "order by a.assignDate desc";
    private static final String GET_USERS_ASSIGNS_QUERY = "from " + RoleAssign.class.getSimpleName() + " a "
            + "where a.serverId = :serverId and a.userId = :userId";
    private static final String GET_EXPIRED_QUEUE_QUERY = "from " + RoleAssign.class.getSimpleName() + " a "
            + "where a.serverId = :serverId and a.assignDate <= :assignDate";

    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Role assign queue");

    RoleAssignDAOImpl(@NotNull AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void addToQueue(long serverId, long userId, long roleId, @NotNull Instant assignTime) {
        try (AutoSession session = sessionFactory.openSession()) {
            RoleAssign roleAssign = new RoleAssign();
            roleAssign.setServerId(serverId);
            roleAssign.setUserId(userId);
            roleAssign.setRoleId(roleId);
            roleAssign.setAssignDate(Timestamp.from(assignTime));
            roleAssign.setCreateDate(Timestamp.from(Instant.now()));
            session.save(roleAssign);
        } catch (Exception err) {
            String errMsg = String.format("Unable to queue role assign for user id %d, role id %d, " +
                    "server id %d, assign time \"%s\": %s", userId, roleId, serverId, assignTime.toString(), err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public void clearQueueFor(long serverId, long userId) {
        try (AutoSession session = sessionFactory.openSession()) {
            List<RoleAssign> assigns = session.createQuery(GET_USERS_ASSIGNS_QUERY, RoleAssign.class)
                    .setParameter("serverId", serverId)
                    .setParameter("userId", userId)
                    .list();
            session.removeAll(assigns);
        } catch (Exception err) {
            String errMsg = String.format("Unable to remove all role assigns for user id %d, server id %d: %s",
                    userId, serverId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public @UnmodifiableView @NotNull List<RoleAssign> getQueueFor(long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {
            List<RoleAssign> assigns = session.createQuery(LOAD_FULL_QUEUE_QUERY, RoleAssign.class)
                    .setParameter("serverId", serverId)
                    .list();
            if (assigns != null && !assigns.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Found {} role assigns for server id {}", assigns.size(), serverId);
                }
                return Collections.unmodifiableList(assigns);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No role assigns found for server id {}", serverId);
                }
                return Collections.emptyList();
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to load role assigns queue for server id %d: %s", serverId,
                    err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Collections.emptyList();
        }
    }

    @Override
    public @UnmodifiableView @NotNull List<RoleAssign> getTimeoutReached(long serverId, @NotNull Instant time) {
        try (AutoSession session = sessionFactory.openSession()) {
            List<RoleAssign> assigns = session.createQuery(GET_EXPIRED_QUEUE_QUERY, RoleAssign.class)
                    .setParameter("serverId", serverId)
                    .setParameter("assignDate", Timestamp.from(time))
                    .list();
            if (assigns != null && !assigns.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Found {} role assigns for server id {}", assigns.size(), serverId);
                }
                session.removeAll(assigns);
                return Collections.unmodifiableList(assigns);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No role assigns found for server id {}", serverId);
                }
                return Collections.emptyList();
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to load and delete timeout reached " +
                    "role assigns queue for server id %d: %s", serverId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Collections.emptyList();
        }
    }

    @Override
    public @UnmodifiableView @NotNull List<RoleAssign> getTimeoutReached(long serverId) {
        return getTimeoutReached(serverId, Instant.now());
    }

    @Override
    public void discardRoleAssign(@NotNull RoleAssign roleAssign) {
        if (roleAssign.getId() == 0L) {
            return;
        }
        try (AutoSession session = sessionFactory.openSession()) {
            RoleAssign stored = session.find(RoleAssign.class, roleAssign.getId());
            session.remove(stored);
        } catch (Exception err) {
            String errMsg = String.format("Unable to discard queued role assign \"%s\": %s", roleAssign, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }
}

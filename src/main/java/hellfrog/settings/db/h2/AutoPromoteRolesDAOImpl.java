package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import hellfrog.settings.db.AutoPromoteRolesDAO;
import hellfrog.settings.db.entity.AutoPromoteConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

class AutoPromoteRolesDAOImpl
        implements AutoPromoteRolesDAO {

    private static final String LOAD_CONFIGS_QUERY = "from " + AutoPromoteConfig.class.getSimpleName() + " c "
            + "where c.serverId = :serverId";
    private static final String GET_CONFIG_QUERY = "from " + AutoPromoteConfig.class.getSimpleName() + " c "
            + "where c.serverId = :serverId and c.roleId = :roleId";

    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Auto promote roles");

    AutoPromoteRolesDAOImpl(@NotNull AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public @UnmodifiableView @NotNull List<AutoPromoteConfig> loadAllConfigs(final long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {
            List<AutoPromoteConfig> result = session.createQuery(LOAD_CONFIGS_QUERY, AutoPromoteConfig.class)
                    .setParameter("serverId", serverId)
                    .list();
            if (result == null || result.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("No auto promotes found for server {}", serverId);
                }
                return Collections.emptyList();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Found {} auto promotes for server {}", result.size(), serverId);
                }
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to load auto promote configs for server %d: %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Collections.emptyList();
        }
    }

    @Override
    public void addConfig(final long serverId, final long roleId, final long timeoutSeconds) {
        try (AutoSession session = sessionFactory.openSession()) {
            AutoPromoteConfig config = session.createQuery(GET_CONFIG_QUERY, AutoPromoteConfig.class)
                    .setParameter("serverId", serverId)
                    .setParameter("roleId", roleId)
                    .uniqueResult();
            if (config == null) {
                config = new AutoPromoteConfig();
                config.setCreateDate(Timestamp.from(Instant.now()));
                config.setServerId(serverId);
                config.setRoleId(roleId);
            }
            config.setTimeout(timeoutSeconds);
            session.save(config);
        } catch (Exception err) {
            String errMsg = String.format("Unable to add auto promote config, server id %d, role id %d, " +
                    "timeout %d sec.: %s", serverId, roleId, timeoutSeconds, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public void deleteConfig(final long serverId, final long roleId) {
        try (AutoSession session = sessionFactory.openSession()) {
            AutoPromoteConfig config = session.createQuery(GET_CONFIG_QUERY, AutoPromoteConfig.class)
                    .setParameter("serverId", serverId)
                    .setParameter("roleId", roleId)
                    .uniqueResult();
            if (config != null) {
                session.remove(config);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to delete auto promote config, server id %d, role id %d: %s",
                    serverId, roleId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }
}

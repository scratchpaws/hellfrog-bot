package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import hellfrog.settings.db.CommunityControlDAO;
import hellfrog.settings.db.entity.CommunityControlSettings;
import hellfrog.settings.db.entity.CommunityControlUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class CommunityControlDAOImpl
        implements CommunityControlDAO {

    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Community control DB");

    private static final String GET_SETTINGS_QUERY = "from " + CommunityControlSettings.class.getSimpleName() + " s "
            + "where s.serverId = :serverId";
    private static final String GET_USER_IDS_QUERY = "select u.userId "
            + "from " + CommunityControlUser.class.getSimpleName() + " u "
            + "where u.serverId = :serverId";
    private static final String GET_COMMUNITY_CONTROL_USER_QUERY = "from " + CommunityControlUser.class.getSimpleName() + " u "
            + "where u.serverId = :serverId and u.userId = :userId";

    CommunityControlDAOImpl(@NotNull AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @NotNull
    public Optional<CommunityControlSettings> getSettings(final long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {
            return session.createQuery(GET_SETTINGS_QUERY, CommunityControlSettings.class)
                    .setParameter("serverId", serverId)
                    .uniqueResultOptional();
        } catch (Exception err) {
            String errMsg = String.format("Unable to fetch community control settings for server id %d: %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Optional.empty();
        }
    }

    public void setSettings(@NotNull final CommunityControlSettings settings) {
        try (AutoSession session = sessionFactory.openSession()) {
            CommunityControlSettings stored = session.createQuery(GET_SETTINGS_QUERY, CommunityControlSettings.class)
                    .setParameter("serverId", settings.getServerId())
                    .uniqueResult();
            if (stored == null) {
                stored = new CommunityControlSettings();
                stored.setServerId(settings.getServerId());
                stored.setCreateDate(Timestamp.from(Instant.now()));
            }
            stored.setRoleId(settings.getRoleId());
            stored.setThreshold(settings.getThreshold());
            stored.setUnicodeEmoji(settings.getUnicodeEmoji());
            stored.setCustomEmojiId(settings.getCustomEmojiId());
            stored.setUpdateDate(Timestamp.from(Instant.now()));
            session.save(stored);
        } catch (Exception err) {
            String errMsg = String.format("Unable to save community control settings \"%s\": %s",
                    settings.toString(), err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @NotNull
    @UnmodifiableView
    public List<Long> getUsers(final long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {
            List<Long> result = session.createQuery(GET_USER_IDS_QUERY, Long.class)
                    .setParameter("serverId", serverId)
                    .list();
            if (result == null || result.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("No community control users found for server id {}", serverId);
                }
                return Collections.emptyList();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Found {} community control users for server id {}", result.size(), serverId);
                }
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to fetch community control users for server id %d: %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Collections.emptyList();
        }
    }

    public void addUser(final long serverId, final long userId) {
        try (AutoSession session = sessionFactory.openSession()) {
            CommunityControlUser user = session.createQuery(GET_COMMUNITY_CONTROL_USER_QUERY, CommunityControlUser.class)
                    .setParameter("serverId", serverId)
                    .setParameter("userId", userId)
                    .uniqueResult();
            if (user == null) {
                user = new CommunityControlUser();
                user.setServerId(serverId);
                user.setUserId(userId);
                user.setCreateDate(Timestamp.from(Instant.now()));
                session.save(user);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to add user to community control users list, server id %d, user id %d: %s",
                    serverId, userId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    public void removeUser(final long serverId, final long userId) {
        try (AutoSession session = sessionFactory.openSession()) {
            CommunityControlUser user = session.createQuery(GET_COMMUNITY_CONTROL_USER_QUERY, CommunityControlUser.class)
                    .setParameter("serverId", serverId)
                    .setParameter("userId", userId)
                    .uniqueResult();
            if (user != null) {
                session.remove(user);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to remove user from community control users list, server id %d, user id %d: %s",
                    serverId, userId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }
}

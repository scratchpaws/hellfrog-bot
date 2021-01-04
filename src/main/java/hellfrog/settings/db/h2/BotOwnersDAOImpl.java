package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import hellfrog.settings.db.BotOwnersDAO;
import hellfrog.settings.db.entity.BotOwner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BotOwnersDAOImpl
        implements BotOwnersDAO {

    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Bot owners");

    BotOwnersDAOImpl(@NotNull AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Long> getAll() {
        try (AutoSession session = sessionFactory.openSession()) {
            List<BotOwner> allOwners = session.getAll(BotOwner.class);
            if (allOwners == null || allOwners.isEmpty()) {
                return Collections.emptyList();
            } else {
                List<Long> result = new ArrayList<>(allOwners.size());
                for (BotOwner botOwner : allOwners) {
                    result.add(botOwner.getUserId());
                }
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to get all bot owners: %s", err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isPresent(long userId) {
        try (AutoSession session = sessionFactory.openSession()) {
            List<BotOwner> found = session.createQuery("from BotOwner bo where bo.userId = :userId", BotOwner.class)
                    .setParameter("userId", userId)
                    .list();
            session.success();
            int count = found != null && !found.isEmpty() ? found.size() : 0;
            if (log.isDebugEnabled()) {
                log.debug("Found {} values for user id {} into bot owners", count, userId);
            }
            if (log.isDebugEnabled() && count == 0) {
                log.debug("No values found for user id {} into bot owners", userId);
            }
            return count > 0;
        } catch (Exception err) {
            String errMsg = String.format("Unable to check what user with id %d is present into bot owners: %s",
                    userId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
        return false;
    }

    @Override
    public boolean addToOwners(long userId) {
        if (!isPresent(userId)) {
            BotOwner botOwner = new BotOwner();
            botOwner.setUserId(userId);
            botOwner.setCreateDate(Timestamp.from(Instant.now()));
            try (AutoSession session = sessionFactory.openSession()) {
                session.save(botOwner);
            } catch (Exception err) {
                String errMsg = String.format("Unable to add %d to global bot owners: %s", userId, err.getMessage());
                log.error(errMsg, err);
                LogsStorage.addErrorMessage(errMsg);
                return false;
            }
            return isPresent(userId);
        }
        return false;
    }

    @Override
    public boolean deleteFromOwners(long userId) {
        if (isPresent(userId)) {
            try (AutoSession session = sessionFactory.openSession()) {
                int deletedRows = session.createQuery("delete from BotOwner bo where bo.userId = :userId")
                        .setParameter("userId", userId)
                        .executeUpdate();
                session.success();
                if (log.isDebugEnabled()) {
                    log.debug("Deleted {} values", deletedRows);
                }
            } catch (Exception err) {
                String errMsg = String.format("Unable to delete %d from global bot owners: %s", userId, err.getMessage());
                log.error(errMsg, err);
                LogsStorage.addErrorMessage(errMsg);
            }
            return !isPresent(userId);
        }
        return false;
    }
}

package hellfrog.settings.db.h2;

import hellfrog.common.CommonUtils;
import hellfrog.core.LogsStorage;
import hellfrog.settings.db.AddUpdateState;
import hellfrog.settings.db.WtfAssignDAO;
import hellfrog.settings.db.entity.WtfEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class WtfAssignDAOImpl
        implements WtfAssignDAO {

    private final Logger log = LogManager.getLogger("WTF assign");
    private final AutoSessionFactory sessionFactory;

    WtfAssignDAOImpl(@NotNull AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private List<WtfEntry> getAssigns(long serverId, long userId, boolean onlyFirst) {
        try (AutoSession session = sessionFactory.openSession()) {
            Query<WtfEntry> query = session.createQuery("from " + WtfEntry.class.getSimpleName() + " w " +
                    "where w.serverId = :serverId and w.targetId = :targetId " +
                    "order by w.updateDate desc, w.id desc", WtfEntry.class)
                    .setParameter("serverId", serverId)
                    .setParameter("targetId", userId);
            if (onlyFirst) {
                query.setMaxResults(1);
            }
            List<WtfEntry> result = query.list();
            if (result == null || result.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable get all wtf for server id %d and user id %d: %s",
                    serverId, userId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<WtfEntry> getLatest(long serverId, long userId) {
        List<WtfEntry> wtfEntryList = getAssigns(serverId, userId, true);
        return CommonUtils.getFirstOrEmpty(wtfEntryList);
    }

    @Override
    public List<WtfEntry> getAll(long serverId, long userId) {
        return getAssigns(serverId, userId, false);
    }

    @Override
    public AddUpdateState addOrUpdate(long serverId, long userId, @NotNull WtfEntry wtfEntry) {

        if (CommonUtils.isTrStringEmpty(wtfEntry.getDescription()) && CommonUtils.isTrStringEmpty(wtfEntry.getImageUri())) {
            return remove(serverId, wtfEntry.getAuthorId(), userId);
        }

        try (AutoSession session = sessionFactory.openSession()) {

            int updatedCount = session.createQuery("update " + wtfEntry.getClass().getSimpleName() + " w " +
                    "set w.description = :description, w.imageUri = :imageUri, w.updateDate = :updateDate " +
                    "where w.serverId = :serverId and w.authorId = :authorId and w.targetId = :targetId")
                    .setParameter("description", wtfEntry.getDescription())
                    .setParameter("imageUri", wtfEntry.getImageUri())
                    .setParameter("updateDate", wtfEntry.getUpdateDate() != null ? wtfEntry.getUpdateDate() : Timestamp.from(Instant.now()))
                    .setParameter("serverId", serverId)
                    .setParameter("authorId", wtfEntry.getAuthorId())
                    .setParameter("targetId", userId)
                    .executeUpdate();

            if (updatedCount == 1) {
                session.success();
                return AddUpdateState.UPDATED;
            } else if (updatedCount > 1) {
                throw new SQLException(String.format("Present non-unique values of server id \"%d\" and user id \"%d\" and " +
                        "author id \"%d\"", serverId, userId, wtfEntry.getAuthorId()));
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to update wtf entry for server id \"%d\" and user id \"%d\" with " +
                    "%s: %s", serverId, userId, wtfEntry, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return AddUpdateState.ERROR;
        }

        try (AutoSession session = sessionFactory.openSession()) {
            List<WtfEntry> entries = session.createQuery("from " + WtfEntry.class.getSimpleName() + " w " +
                            "where w.serverId = :serverId and w.authorId = :authorId and w.targetId = :targetId",
                    WtfEntry.class)
                    .setParameter("serverId", serverId)
                    .setParameter("authorId", wtfEntry.getAuthorId())
                    .setParameter("targetId", userId)
                    .list();
            Optional<WtfEntry> mayBeEntry = CommonUtils.getFirstOrEmpty(entries);
            WtfEntry entry = mayBeEntry.orElse(wtfEntry);
            if (mayBeEntry.isPresent()) {
                entry.setDescription(wtfEntry.getDescription());
                entry.setImageUri(wtfEntry.getImageUri());
            } else {
                entry.setServerId(serverId);
                entry.setTargetId(userId);
            }
            entry.setUpdateDate(wtfEntry.getUpdateDate() != null ? wtfEntry.getUpdateDate() : Timestamp.from(Instant.now()));
            entry.setCreateDate(wtfEntry.getCreateDate() != null ? wtfEntry.getCreateDate() : Timestamp.from(Instant.now()));
            session.save(entry);
            return mayBeEntry.isEmpty() ? AddUpdateState.ADDED : AddUpdateState.UPDATED;
        } catch (Exception err) {
            String errMsg = String.format("Unable to insert wtf entry for server id \"%d\" and user id \"%d\" with " +
                    "%s: %s", serverId, userId, wtfEntry, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return AddUpdateState.ERROR;
        }
    }

    @Override
    public AddUpdateState remove(long serverId, long authorId, long userId) {

        try (AutoSession session = sessionFactory.openSession()) {

            int deletedCount = session.createQuery("delete from " + WtfEntry.class.getSimpleName() + " w " +
                    "where w.serverId = :serverId and w.authorId = :authorId and w.targetId = :targetId")
                    .setParameter("serverId", serverId)
                    .setParameter("authorId", authorId)
                    .setParameter("targetId", userId)
                    .executeUpdate();
            if (deletedCount > 0) {
                session.success();
            }
            return deletedCount > 0 ? AddUpdateState.REMOVED : AddUpdateState.NO_CHANGES;
        } catch (Exception err) {
            String errMsg = String.format("Unable delete wtf entry for server id \"%d\" and user id \"%d\" " +
                    "and author id \"%d\": %s", serverId, userId, authorId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return AddUpdateState.ERROR;
        }
    }
}

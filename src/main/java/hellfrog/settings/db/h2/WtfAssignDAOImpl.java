package hellfrog.settings.db.h2;

import hellfrog.common.CommonUtils;
import hellfrog.core.LogsStorage;
import hellfrog.settings.db.AddUpdateState;
import hellfrog.settings.db.WtfAssignDAO;
import hellfrog.settings.db.entity.WtfEntry;
import hellfrog.settings.db.entity.WtfEntryAttach;
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
import java.util.Set;

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
    public AddUpdateState addOrUpdate(@NotNull WtfEntry wtfEntry) {

        if (CommonUtils.isTrStringEmpty(wtfEntry.getDescription())
                && (wtfEntry.getAttaches() == null || wtfEntry.getAttaches().isEmpty())) {
            return remove(wtfEntry.getServerId(), wtfEntry.getAuthorId(), wtfEntry.getTargetId());
        }

        try (AutoSession session = sessionFactory.openSession()) {
            List<WtfEntry> found = session.createQuery("from " + wtfEntry.getClass().getSimpleName() + " w " +
                    "where w.serverId = :serverId and w.authorId = :authorId and w.targetId = :targetId", WtfEntry.class)
                    .setParameter("serverId", wtfEntry.getServerId())
                    .setParameter("authorId", wtfEntry.getAuthorId())
                    .setParameter("targetId", wtfEntry.getTargetId())
                    .list();

            if (found != null && !found.isEmpty()) {
                if (found.size() > 1) {
                    throw new SQLException(String.format("Present non-unique values of server id \"%d\" and user id \"%d\" and " +
                            "author id \"%d\"", wtfEntry.getServerId(), wtfEntry.getTargetId(), wtfEntry.getAuthorId()));
                }

                WtfEntry entry = found.get(0);
                session.removeAll(entry.getAttaches());
                entry.setUpdateDate(Timestamp.from(Instant.now()));
                entry.setDescription(wtfEntry.getDescription());
                replaceAttachments(wtfEntry, entry);
                saveEntry(session, entry);
                return AddUpdateState.UPDATED;
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to update wtf entry for server id \"%d\" and user id \"%d\" with " +
                    "%s: %s", wtfEntry.getServerId(), wtfEntry.getTargetId(), wtfEntry, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return AddUpdateState.ERROR;
        }

        try (AutoSession session = sessionFactory.openSession()) {
            List<WtfEntry> entries = session.createQuery("from " + WtfEntry.class.getSimpleName() + " w " +
                            "where w.serverId = :serverId and w.authorId = :authorId and w.targetId = :targetId",
                    WtfEntry.class)
                    .setParameter("serverId", wtfEntry.getServerId())
                    .setParameter("authorId", wtfEntry.getAuthorId())
                    .setParameter("targetId", wtfEntry.getTargetId())
                    .list();
            Optional<WtfEntry> mayBeEntry = CommonUtils.getFirstOrEmpty(entries);
            WtfEntry entry = mayBeEntry.orElse(wtfEntry);
            if (mayBeEntry.isPresent()) {
                entry.setDescription(wtfEntry.getDescription());
                session.removeAll(entry.getAttaches());
            } else {
                entry.setId(0L);
            }
            replaceAttachments(wtfEntry, entry);
            entry.setUpdateDate(wtfEntry.getUpdateDate() != null ? wtfEntry.getUpdateDate() : Timestamp.from(Instant.now()));
            entry.setCreateDate(wtfEntry.getCreateDate() != null ? wtfEntry.getCreateDate() : Timestamp.from(Instant.now()));
            saveEntry(session, entry);
            return mayBeEntry.isEmpty() ? AddUpdateState.ADDED : AddUpdateState.UPDATED;
        } catch (Exception err) {
            String errMsg = String.format("Unable to insert wtf entry for server id \"%d\" and user id \"%d\" with " +
                    "%s: %s", wtfEntry.getServerId(), wtfEntry.getTargetId(), wtfEntry, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return AddUpdateState.ERROR;
        }
    }

    private void saveEntry(AutoSession session, WtfEntry entry) {
        session.save(entry);
        boolean saveAttachments = false;
        for (WtfEntryAttach attach : entry.getAttaches()) {
            if (attach.getId() == 0L) {
                saveAttachments = true;
                break;
            }
        }
        if (saveAttachments) {
            session.saveAll(entry.getAttaches());
        }
    }

    private void replaceAttachments(@NotNull WtfEntry fromEntry, @NotNull WtfEntry toEntry) {
        Set<WtfEntryAttach> attaches = Collections.emptySet();
        if (fromEntry.getAttaches() != null && !fromEntry.getAttaches().isEmpty()) {
            attaches = fromEntry.getAttaches();
            for (WtfEntryAttach attach : attaches) {
                attach.setWtfEntry(toEntry);
                attach.setId(0L);
                attach.setCreateDate(Timestamp.from(Instant.now()));
            }
        }
        toEntry.setAttaches(attaches);
    }

    @Override
    public AddUpdateState remove(long serverId, long authorId, long userId) {

        try (AutoSession session = sessionFactory.openSession()) {

            List<WtfEntry> found = session.createQuery("from " + WtfEntry.class.getSimpleName() + " w " +
                    "where w.serverId = :serverId and w.authorId = :authorId and w.targetId = :targetId", WtfEntry.class)
                    .setParameter("serverId", serverId)
                    .setParameter("authorId", authorId)
                    .setParameter("targetId", userId)
                    .list();

            for (WtfEntry entry : found) {
                session.removeAll(entry.getAttaches());
            }
            session.removeAll(found);
            return !found.isEmpty() ? AddUpdateState.REMOVED : AddUpdateState.NO_CHANGES;

        } catch (Exception err) {
            String errMsg = String.format("Unable delete wtf entry for server id \"%d\" and user id \"%d\" " +
                    "and author id \"%d\": %s", serverId, userId, authorId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return AddUpdateState.ERROR;
        }
    }
}

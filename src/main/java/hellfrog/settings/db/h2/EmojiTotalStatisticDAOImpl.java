package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import hellfrog.settings.db.EmojiTotalStatisticDAO;
import hellfrog.settings.db.entity.EmojiTotalStatistic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class EmojiTotalStatisticDAOImpl
        implements EmojiTotalStatisticDAO {

    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Emoji total statistic");

    EmojiTotalStatisticDAOImpl(AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public long getEmojiUsagesCount(long serverId, long emojiId) {
        try (AutoSession session = sessionFactory.openSession()) {
            Optional<EmojiTotalStatistic> result = findUsagesCount(session, serverId, emojiId);
            if (log.isDebugEnabled()) {
                if (result.isEmpty()) {
                    log.debug("Server id: {}, emoji id: {}, no usages count found",
                            serverId, emojiId);
                } else {
                    log.debug("Server id: {}, emoji id: {}, usages count is {}",
                            serverId, emojiId, result.get());
                }
            }
            return result.map(EmojiTotalStatistic::getUsagesCount).orElse(NO_USAGES_FOUND);
        } catch (Exception err) {
            String errMsg = String.format("Unable to get total emoji usages count for server id \"%d\" and emoji id \"%d\": %s",
                    serverId, emojiId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return ERR_REACHED;
        }
    }

    private Optional<EmojiTotalStatistic> findUsagesCount(@NotNull final AutoSession session,
                                                          final long serverId,
                                                          final long emojiId) {

        List<EmojiTotalStatistic> result = session.createQuery("from " + EmojiTotalStatistic.class.getSimpleName() + " es " +
                "where es.serverId = :serverId and es.emojiId = :emojiId", EmojiTotalStatistic.class)
                .setParameter("serverId", serverId)
                .setParameter("emojiId", emojiId)
                .list();

        if (result == null || result.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(result.get(0));
        }
    }

    @Override
    public void insertStats(long serverId, long emojiId, long usagesCount, @NotNull Instant lastUsage) {
        try (AutoSession session = sessionFactory.openSession()) {
            Optional<EmojiTotalStatistic> result = findUsagesCount(session, serverId, emojiId);
            EmojiTotalStatistic current = result.orElse(new EmojiTotalStatistic());
            current.setServerId(serverId);
            current.setEmojiId(emojiId);
            current.setUsagesCount(usagesCount);
            current.setLastUsage(Timestamp.from(lastUsage));
            current.setCreateDate(Timestamp.from(Instant.now()));
            current.setUpdateDate(Timestamp.from(Instant.now()));
            session.save(current);
        } catch (Exception err) {
            String errMsg = String.format("Unable to insert total emoji usages count for server id: \"%d\", " +
                            "emoji id: \"%d\", usages count: \"%d\", last usage: \"%s\": %s",
                    serverId, emojiId, usagesCount, lastUsage, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public void updateStats(long serverId, long emojiId, @NotNull Instant lastUsage, boolean increment) {
        boolean present = false;
        try (AutoSession session = sessionFactory.openSession()) {
            Optional<EmojiTotalStatistic> result = findUsagesCount(session, serverId, emojiId);
            if (result.isPresent()) {
                present = true;
                EmojiTotalStatistic current = result.get();
                long usagesCount;
                if (increment) {
                    usagesCount = current.getUsagesCount() + 1L;
                    current.setLastUsage(Timestamp.from(lastUsage));
                } else {
                    usagesCount = current.getUsagesCount() > 0L ? current.getUsagesCount() - 1L : 0L;
                }
                current.setUsagesCount(usagesCount);
                current.setUpdateDate(Timestamp.from(Instant.now()));
                session.save(current);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to update total emoji usages count for server id: \"%d\", " +
                            "emoji id: \"%d\", last usage: \"%s\": %s",
                    serverId, emojiId, lastUsage, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
        if (!present) {
            insertStats(serverId, emojiId, (increment ? 1L : 0L), lastUsage);
        }
    }

    @Override
    public void increment(long serverId, long emojiId) {
        updateStats(serverId, emojiId, Instant.now(), true);
    }

    @Override
    public void incrementWithLastDate(long serverId, long emojiId, @NotNull Instant lastDate) {
        updateStats(serverId, emojiId, lastDate, true);
    }

    @Override
    public void decrement(long serverId, long emojiId) {
        updateStats(serverId, emojiId, Instant.now(), false);
    }

    @Override
    public List<EmojiTotalStatistic> getAllEmojiUsagesStatistic(long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {
            List<EmojiTotalStatistic> result = session.createQuery("from " + EmojiTotalStatistic.class.getSimpleName() + " es " +
                    "where es.serverId = :serverId", EmojiTotalStatistic.class)
                    .setParameter("serverId", serverId)
                    .list();

            if (result == null || result.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Extracted empty emoji stats for server with id {}", serverId);
                }
                return Collections.emptyList();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Extracted {} emoji stats for server with id {}", result.size(), serverId);
                }
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to fetch all total emoji statistic usages for server with id \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Collections.emptyList();
        }
    }
}

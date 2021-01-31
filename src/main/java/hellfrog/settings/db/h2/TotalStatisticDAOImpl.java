package hellfrog.settings.db.h2;

import hellfrog.core.LogsStorage;
import hellfrog.settings.db.TotalStatisticDAO;
import hellfrog.settings.db.entity.EmojiTotalStatistic;
import hellfrog.settings.db.entity.TextChannelTotalStatistic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

class TotalStatisticDAOImpl
        implements TotalStatisticDAO {

    private static final boolean INCREMENT = true;
    private static final boolean DECREMENT = false;
    private static final String FIND_EMOJI_USAGES_QUERY = "from " + EmojiTotalStatistic.class.getSimpleName() + " es " +
            "where es.serverId = :serverId and es.emojiId = :emojiId";
    private static final String GET_ALL_EMOJI_USAGES_QUERY = "from " + EmojiTotalStatistic.class.getSimpleName() + " es " +
            "where es.serverId = :serverId";
    private static final String GET_ALL_CHANNEL_STATISTIC_QUERY = "from " + TextChannelTotalStatistic.class.getSimpleName() + " ts " +
            "where ts.serverId = :serverId";
    private static final String FIND_CHANNEL_STATISTIC_QUERY = "from " + TextChannelTotalStatistic.class.getSimpleName() + " ts " +
            "where ts.serverId = :serverId and ts.textChannelId = :textChannelId and ts.userId = :userId";
    private static final List<String> RESET_QUERIES = List.of(
            "delete from " + EmojiTotalStatistic.class.getSimpleName() + " s " +
                    "where s.serverId = :serverId",
            "delete from " + TextChannelTotalStatistic.class.getSimpleName() + " s " +
                    "where s.serverId = :serverId"
    );
    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Total statistic");

    TotalStatisticDAOImpl(@NotNull final AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Nullable
    private EmojiTotalStatistic findEmojiStatistic(@NotNull final AutoSession session,
                                                   final long serverId,
                                                   final long emojiId) {

        return session.createQuery(FIND_EMOJI_USAGES_QUERY, EmojiTotalStatistic.class)
                .setParameter("serverId", serverId)
                .setParameter("emojiId", emojiId)
                .uniqueResult();
    }

    @Override
    public void insertEmojiStats(long serverId, long emojiId, long usagesCount, @NotNull Instant lastUsage) {
        try (AutoSession session = sessionFactory.openSession()) {
            EmojiTotalStatistic current = findEmojiStatistic(session, serverId, emojiId);
            if (current == null) {
                current = new EmojiTotalStatistic();
            }
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

    private void updateEmojiStats(long serverId, long emojiId, @NotNull Instant lastUsage, boolean increment) {
        try (AutoSession session = sessionFactory.openSession()) {
            EmojiTotalStatistic stored = findEmojiStatistic(session, serverId, emojiId);
            if (stored == null) {
                stored = new EmojiTotalStatistic();
                stored.setServerId(serverId);
                stored.setEmojiId(emojiId);
                stored.setCreateDate(Timestamp.from(Instant.now()));
                stored.setUsagesCount(0L);
            }
            long usagesCount = stored.getUsagesCount();
            if (increment) {
                usagesCount++;
                stored.setLastUsage(getLastDate(stored.getLastUsage(), lastUsage));
            } else {
                usagesCount = Math.max(0L, usagesCount - 1);
            }
            stored.setUsagesCount(usagesCount);
            stored.setUpdateDate(Timestamp.from(Instant.now()));
            session.save(stored);
        } catch (Exception err) {
            String errMsg = String.format("Unable to update total emoji usages count for server id: \"%d\", " +
                            "emoji id: \"%d\", last usage: \"%s\": %s",
                    serverId, emojiId, lastUsage, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public void incrementEmoji(long serverId, long emojiId) {
        updateEmojiStats(serverId, emojiId, Instant.now(), INCREMENT);
    }

    @Override
    public void incrementEmojiWithDate(long serverId, long emojiId, @NotNull Instant lastDate) {
        updateEmojiStats(serverId, emojiId, lastDate, INCREMENT);
    }

    @Override
    public void decrementEmoji(long serverId, long emojiId) {
        updateEmojiStats(serverId, emojiId, Instant.now(), DECREMENT);
    }

    @Override
    public List<EmojiTotalStatistic> getEmojiUsagesStatistic(long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {
            List<EmojiTotalStatistic> result = session.createQuery(GET_ALL_EMOJI_USAGES_QUERY, EmojiTotalStatistic.class)
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

    @Override
    public void reset(long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {
            RESET_QUERIES.forEach(query -> session.createQuery(query)
                    .setParameter("serverId", serverId)
                    .executeUpdate());
            session.success();
        } catch (Exception err) {
            String errMsg = String.format("Unable to reset total statistic for server with id %d: %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public void removeEmojiStats(long serverId, long emojiId) {
        try (AutoSession session = sessionFactory.openSession()) {
            EmojiTotalStatistic statistic = session.createQuery(FIND_EMOJI_USAGES_QUERY, EmojiTotalStatistic.class)
                    .setParameter("serverId", serverId)
                    .setParameter("emojiId", emojiId)
                    .uniqueResult();
            if (statistic != null) {
                session.remove(statistic);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to delete emoji from total statistic, server id - %d, emoji id - %d: %s",
                    serverId, emojiId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public void insertChannelStats(long serverId,
                                   long textChannelId,
                                   long userId,
                                   long messagesCount,
                                   @NotNull Instant lastMessageDate,
                                   long symbolsCount,
                                   long bytesCount) {

        try (AutoSession session = sessionFactory.openSession()) {
            TextChannelTotalStatistic statistic = findTextChannelStatistic(session, serverId, textChannelId, userId);
            if (statistic == null) {
                statistic = new TextChannelTotalStatistic();
                statistic.setServerId(serverId);
                statistic.setTextChannelId(textChannelId);
                statistic.setUserId(userId);
                statistic.setCreateDate(Timestamp.from(Instant.now()));
            }
            statistic.setUpdateDate(Timestamp.from(Instant.now()));
            statistic.setMessagesCount(messagesCount);
            statistic.setLastMessageDate(Timestamp.from(lastMessageDate));
            statistic.setSymbolsCount(symbolsCount);
            statistic.setBytesCount(bytesCount);
            session.save(statistic);
        } catch (Exception err) {
            String errMsg = String.format("Unable to insert channel message statistics, server id %d, " +
                            "text channel id %d, user id %d, messages count %d, last message date \"%s\", " +
                            "symbols count %d, bytes count %d: %s",
                    serverId, textChannelId, userId, messagesCount, lastMessageDate.toString(), symbolsCount,
                    bytesCount, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @NotNull
    private Timestamp getLastDate(@Nullable Timestamp currentLastDate, @NotNull Instant newLastDate) {
        if (currentLastDate == null) {
            return Timestamp.from(newLastDate);
        }
        Timestamp newLastStamp = Timestamp.from(newLastDate);
        if (newLastStamp.after(currentLastDate)) {
            return newLastStamp;
        } else {
            return currentLastDate;
        }
    }

    @Nullable
    private TextChannelTotalStatistic findTextChannelStatistic(@NotNull AutoSession session,
                                                               long serverId,
                                                               long textChannelId,
                                                               long userId) {

        return session.createQuery(FIND_CHANNEL_STATISTIC_QUERY, TextChannelTotalStatistic.class)
                .setParameter("serverId", serverId)
                .setParameter("textChannelId", textChannelId)
                .setParameter("userId", userId)
                .uniqueResult();
    }

    private void updateChannelStats(final long serverId,
                                    final long textChannelId,
                                    final long userId,
                                    final @NotNull Instant lastDate,
                                    final int messageLength,
                                    final long bytesCount,
                                    final boolean increment) {

        try (AutoSession session = sessionFactory.openSession()) {
            TextChannelTotalStatistic current = findTextChannelStatistic(session, serverId, textChannelId, userId);
            if (current == null) {
                current = new TextChannelTotalStatistic();
                current.setServerId(serverId);
                current.setTextChannelId(textChannelId);
                current.setUserId(userId);
                current.setCreateDate(Timestamp.from(Instant.now()));
                current.setLastMessageDate(Timestamp.from(lastDate));
                current.setMessagesCount(0L);
                current.setBytesCount(0L);
                current.setSymbolsCount(0L);
            }
            current.setUpdateDate(Timestamp.from(Instant.now()));
            long totalMessagesCount = current.getMessagesCount();
            long totalSymbolsCount = current.getSymbolsCount();
            long totalBytesCount = current.getBytesCount();
            if (increment) {
                current.setLastMessageDate(getLastDate(current.getLastMessageDate(), lastDate));
                totalMessagesCount++;
                totalSymbolsCount += messageLength;
                totalBytesCount += bytesCount;
            } else {
                totalMessagesCount = Math.max(0L, totalMessagesCount - 1);
                totalSymbolsCount = Math.max(0L, totalSymbolsCount - messageLength);
                totalBytesCount = Math.max(0L, totalBytesCount - bytesCount);
            }
            current.setMessagesCount(totalMessagesCount);
            current.setSymbolsCount(totalSymbolsCount);
            current.setBytesCount(totalBytesCount);
            session.save(current);
        } catch (Exception err) {
            String errMsg = String.format("Unable to  channel message statistics, server id %d, " +
                            "text channel id %d, user id %d: %s",
                    serverId, textChannelId, userId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
        }
    }

    @Override
    public void incrementChannelStatsWithDate(long serverId,
                                              long textChannelId,
                                              long userId,
                                              @NotNull Instant lastDate,
                                              int messageLength,
                                              long bytesCount) {

        updateChannelStats(serverId, textChannelId, userId, lastDate, messageLength, bytesCount, INCREMENT);
    }

    @Override
    public void incrementChannelStats(long serverId,
                                      long textChannelId,
                                      long userId,
                                      int messageLength,
                                      long bytesCount) {
        updateChannelStats(serverId, textChannelId, userId, Instant.now(), messageLength, bytesCount, INCREMENT);
    }

    @Override
    public void decrementChannelStats(long serverId,
                                      long textChannelId,
                                      long userId,
                                      int messageLength,
                                      long bytesCount) {
        updateChannelStats(serverId, textChannelId, userId, Instant.now(), messageLength, bytesCount, DECREMENT);
    }

    @Override
    public List<TextChannelTotalStatistic> getChannelsStatistics(long serverId) {
        try (AutoSession session = sessionFactory.openSession()) {

            List<TextChannelTotalStatistic> result = session.createQuery(GET_ALL_CHANNEL_STATISTIC_QUERY, TextChannelTotalStatistic.class)
                    .setParameter("serverId", serverId)
                    .list();

            if (result == null || result.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Extracted empty text chats statistic for server with id {}", serverId);
                }
                return Collections.emptyList();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Extracted {} text chats statistic for server with id {}", result.size(), serverId);
                }
                return Collections.unmodifiableList(result);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to fetch all text chats statistic for server with id \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
            LogsStorage.addErrorMessage(errMsg);
            return Collections.emptyList();
        }
    }
}

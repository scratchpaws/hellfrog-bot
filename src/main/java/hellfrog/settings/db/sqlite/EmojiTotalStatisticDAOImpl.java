package hellfrog.settings.db.sqlite;

import hellfrog.common.ResourcesLoader;
import hellfrog.settings.db.EmojiTotalStatisticDAO;
import hellfrog.settings.db.entity.EmojiTotalStatistic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Emoji usages total statistic repository class.
 * CREATE TABLE "emoji_total_statistics"
 * (
 * "id"           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
 * "server_id"    INTEGER NOT NULL,
 * "emoji_id"     INTEGER NOT NULL,
 * "usages_count" INTEGER NOT NULL,
 * "last_usage"   INTEGER NOT NULL,
 * "create_date"  INTEGER NOT NULL DEFAULT 0,
 * "update_date"  INTEGER NOT NULL DEFAULT 0
 * );
 */
public class EmojiTotalStatisticDAOImpl
        implements EmojiTotalStatisticDAO {

    private final Connection connection;
    private final Logger log = LogManager.getLogger("Emoji total statistic");

    private final String insertQuery = ResourcesLoader.fromTextFile("sql/statistics/emoji_total_insert_query.sql");
    private final String getEmojiUsagesCountQuery = ResourcesLoader.fromTextFile("sql/statistics/emoji_total_get_emoji_usages_count_query.sql");
    private final String incrementUsagesCount = ResourcesLoader.fromTextFile("sql/statistics/emoji_total_increment_usages_count_query.sql");
    private final String decrementUsagesCount = ResourcesLoader.fromTextFile("sql/statistics/emoji_total_decrement_usages_count_query.sql");
    private final String getAllEmojiUsagesStatisticQuery = ResourcesLoader.fromTextFile("sql/statistics/emoji_total_get_all_usages_stat_query.sql");

    public EmojiTotalStatisticDAOImpl(@NotNull Connection connection) {
        this.connection = connection;
    }

    /**
     * Extract emoji usages count from total server statistic.
     *
     * @param serverId discord server id
     * @param emojiId  known custom emoji id
     * @return usages count if found, <code>0</code> if not found, <code>-1</code> if exception reached
     */
    @Override
    public long getEmojiUsagesCount(long serverId, long emojiId) {

        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}",
                    getEmojiUsagesCountQuery, serverId, emojiId);
        }
        long result = NO_USAGES_FOUND;
        try (PreparedStatement statement = connection.prepareStatement(getEmojiUsagesCountQuery)) {
            statement.setLong(1, serverId);
            statement.setLong(2, emojiId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    result = resultSet.getLong(1);
                    if (log.isDebugEnabled()) {
                        log.debug("Server id: {}, emoji id: {}, usages count is {}",
                                serverId, emojiId, result);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Server id: {}, emoji id: {}, no usages count found",
                                serverId, emojiId);
                    }
                }
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to get total emoji usages count for server id \"%d\" and emoji id \"%d\": %s",
                    serverId, emojiId, err.getMessage());
            log.error(errMsg, err);
            result = ERR_REACHED;
        }
        return result;
    }

    @Override
    public void insertStats(long serverId, long emojiId, long usagesCount, @NotNull Instant lastUsage) {

        long currentDateTime = Instant.now().toEpochMilli();
        long lastUsageMilli = lastUsage.toEpochMilli();

        // (server_id, emoji_id, usages_count, last_usage, create_date, update_date)

        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4: {}\nParam 5: {}\nParam 6: {}",
                    insertQuery, serverId, emojiId, usagesCount, lastUsageMilli, currentDateTime, currentDateTime);
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
            insertStatement.setLong(1, serverId);
            insertStatement.setLong(2, emojiId);
            insertStatement.setLong(3, usagesCount);
            insertStatement.setLong(4, lastUsageMilli);
            insertStatement.setLong(5, currentDateTime);
            insertStatement.setLong(6, currentDateTime);
            int count = insertStatement.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Inserted {} values", count);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to insert total emoji usages count for server id: \"%d\", " +
                            "emoji id: \"%d\", usages count: \"%d\", last usage millis: \"%d\": %s",
                    serverId, emojiId, usagesCount, lastUsageMilli, err.getMessage());
            log.error(errMsg, err);
        }
    }

    @Override
    public void updateStats(long serverId, long emojiId, @NotNull Instant lastUsage, boolean increment) {

        long currentDateTime = Instant.now().toEpochMilli();
        long lastUsageMilli = lastUsage.toEpochMilli();

        // set usages_count = usages_count +/- 1,
        //    last_usage   = ?,
        //    update_date  = ?
        //where server_id = ?
        //  and emoji_id = ?

        String query = increment ? incrementUsagesCount : decrementUsagesCount;
        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}\nParam 2: {}\nParam 3: {}\nParam 4: {}",
                    query, lastUsageMilli, currentDateTime, serverId, emojiId);
        }

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, lastUsageMilli);
            statement.setLong(2, currentDateTime);
            statement.setLong(3, serverId);
            statement.setLong(4, emojiId);
            int count = statement.executeUpdate();
            if (count == 0) {
                insertStats(serverId, emojiId, (increment ? 1L : 0L), lastUsage);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to update total emoji usages count for server id: \"%d\", " +
                            "emoji id: \"%d\", last usage millis: \"%d\": %s",
                    serverId, emojiId, lastUsageMilli, err.getMessage());
            log.error(errMsg, err);
        }
    }

    @Override
    public void increment(long serverId, long emojiId) {
        Instant now = Instant.now();
        incrementWithLastDate(serverId, emojiId, now);
    }

    @Override
    public void incrementWithLastDate(long serverId, long emojiId, @NotNull Instant lastDate) {
        updateStats(serverId, emojiId, lastDate, true);
    }

    @Override
    public void decrement(long serverId, long emojiId) {
        Instant now = Instant.now();
        updateStats(serverId, emojiId, now, false);
    }

    @Override
    @UnmodifiableView
    public List<EmojiTotalStatistic> getAllEmojiUsagesStatistic(long serverId) {

        List<EmojiTotalStatistic> result = new ArrayList<>();

        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}", getAllEmojiUsagesStatisticQuery, serverId);
        }

        try (PreparedStatement statement = connection.prepareStatement(getAllEmojiUsagesStatisticQuery)) {
            statement.setLong(1, serverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                EmojiTotalStatistic entity;
                while (resultSet.next()) {
                    // emoji_id, usages_count, last_usage
                    entity = new EmojiTotalStatistic();
                    entity.setId(0L);
                    entity.setEmojiId(resultSet.getLong(1));
                    entity.setUsagesCount(resultSet.getLong(2));
                    entity.setLastUsage(Timestamp.from(Instant.ofEpochMilli(resultSet.getLong(3))));
                    result.add(entity);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Extracted {} emoji stats for server with id {}", result.size(), serverId);
            }
        } catch (SQLException err) {
            String errMsg = String.format("Unable to fetch all total emoji statistic usages for server with id \"%d\": %s",
                    serverId, err.getMessage());
            log.error(errMsg, err);
        }

        return Collections.unmodifiableList(result);
    }
}

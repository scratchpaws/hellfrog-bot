package hellfrog.settings.db;

import hellfrog.common.FromTextFile;
import hellfrog.common.ResourcesLoader;
import hellfrog.settings.entity.EmojiStatistic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
public class EmojiTotalStatisticDAO {

    private static final long NO_USAGES_FOUND = 0L;
    private static final long ERR_REACHED = -1L;

    private final Connection connection;
    private final Logger log = LogManager.getLogger("Emoji total statistic");

    @FromTextFile(fileName = "sql/statistics/emoji_total_insert_query.sql")
    private String insertQuery = null;

    @FromTextFile(fileName = "sql/statistics/emoji_total_get_emoji_usages_count_query.sql")
    private String getEmojiUsagesCountQuery = null;

    @FromTextFile(fileName = "sql/statistics/emoji_total_increment_usages_count_query.sql")
    private String incrementUsagesCount = null;

    @FromTextFile(fileName = "sql/statistics/emoji_total_decrement_usages_count_query.sql")
    private String decrementUsagesCount = null;

    @FromTextFile(fileName = "sql/statistics/emoji_total_get_all_usages_stat_query.sql")
    private String getAllEmojiUsagesStatisticQuery = null;

    public EmojiTotalStatisticDAO(@NotNull Connection connection) {
        this.connection = connection;
        ResourcesLoader.initFileResources(this, EmojiTotalStatisticDAO.class);
    }

    /**
     * Extract emoji usages count from total server statistic.
     *
     * @param serverId discord server id
     * @param emojiId  known custom emoji id
     * @return usages count if found, <code>0</code> if not found, <code>-1</code> if exception reached
     */
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
            statement.setLong(5, emojiId);
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

    public void increment(long serverId, long emojiId) {
        Instant now = Instant.now();
        incrementWithLastDate(serverId, emojiId, now);
    }

    public void incrementWithLastDate(long serverId, long emojiId, @NotNull Instant lastDate) {
        updateStats(serverId, emojiId, lastDate, true);
    }

    public void decrement(long serverId, long emojiId) {
        Instant now = Instant.now();
        updateStats(serverId, emojiId, now, false);
    }

    @UnmodifiableView
    public List<EmojiStatistic> getAllEmojiUsagesStatistic(long serverId) {

        List<EmojiStatistic> result = new ArrayList<>();

        if (log.isDebugEnabled()) {
            log.debug("Query:\n{}\nParam 1: {}", getAllEmojiUsagesStatisticQuery, serverId);
        }

        try (PreparedStatement statement = connection.prepareStatement(getAllEmojiUsagesStatisticQuery)) {
            statement.setLong(1, serverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                long emojiId, usagesCount;
                Instant lastUsage;
                EmojiStatistic entity;
                while (resultSet.next()) {
                    // emoji_id, usages_count, last_usage
                    emojiId = resultSet.getLong(1);
                    usagesCount = resultSet.getLong(2);
                    lastUsage = Instant.ofEpochMilli(resultSet.getLong(3));
                    entity = new EmojiStatistic(serverId, emojiId, usagesCount, lastUsage);
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

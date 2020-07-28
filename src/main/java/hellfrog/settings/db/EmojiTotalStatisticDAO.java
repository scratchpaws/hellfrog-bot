package hellfrog.settings.db;

import hellfrog.settings.db.entity.EmojiStatistic;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public interface EmojiTotalStatisticDAO {

    long getEmojiUsagesCount(long serverId, long emojiId);

    void insertStats(long serverId, long emojiId, long usagesCount, @NotNull Instant lastUsage);

    void updateStats(long serverId, long emojiId, @NotNull Instant lastUsage, boolean increment);

    void increment(long serverId, long emojiId);

    void incrementWithLastDate(long serverId, long emojiId, @NotNull Instant lastDate);

    void decrement(long serverId, long emojiId);

    List<EmojiStatistic> getAllEmojiUsagesStatistic(long serverId);
}

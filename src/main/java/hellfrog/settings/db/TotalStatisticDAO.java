package hellfrog.settings.db;

import hellfrog.settings.db.entity.EmojiTotalStatistic;
import hellfrog.settings.db.entity.TextChannelTotalStatistic;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

public interface TotalStatisticDAO {

    void reset(long serverId);

    void insertEmojiStats(long serverId, long emojiId, long usagesCount, @NotNull Instant lastUsage);

    void incrementEmoji(long serverId, long emojiId);

    void incrementEmojiWithDate(long serverId, long emojiId, @NotNull Instant lastDate);

    void decrementEmoji(long serverId, long emojiId);

    List<EmojiTotalStatistic> getEmojiUsagesStatistic(long serverId);

    void insertChannelStats(long serverId,
                            long textChannelId,
                            long userId,
                            long messagesCount,
                            @NotNull Instant lastMessageDate,
                            long symbolsCount,
                            long bytesCount);

    void incrementChannelStatsWithDate(long serverId,
                                       long textChannelId,
                                       long userId,
                                       @NotNull Instant lastDate,
                                       int messageLength,
                                       long bytesCount);

    void incrementChannelStats(long serverId,
                               long textChannelId,
                               long userId,
                               int messageLength,
                               long bytesCount);

    void decrementChannelStats(long serverId,
                               long textChannelId,
                               long userId,
                               int messageLength,
                               long bytesCount);

    List<TextChannelTotalStatistic> getChannelsStatistics(long serverId);
}

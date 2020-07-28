package hellfrog.settings.db.entity;

import java.time.Instant;
import java.util.Objects;

public class EmojiStatistic {

    private final long serverId;
    private final long emojiId;
    private final long usagesCount;
    private final Instant lastUsage;

    public EmojiStatistic(long serverId, long emojiId, long usagesCount, Instant lastUsage) {
        this.serverId = serverId;
        this.emojiId = emojiId;
        this.usagesCount = usagesCount;
        this.lastUsage = lastUsage;
    }

    public long getServerId() {
        return serverId;
    }

    public long getEmojiId() {
        return emojiId;
    }

    public long getUsagesCount() {
        return usagesCount;
    }

    public Instant getLastUsage() {
        return lastUsage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmojiStatistic that = (EmojiStatistic) o;
        return serverId == that.serverId &&
                emojiId == that.emojiId &&
                usagesCount == that.usagesCount &&
                Objects.equals(lastUsage, that.lastUsage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverId, emojiId, usagesCount, lastUsage);
    }

    @Override
    public String toString() {
        return "EmojiStatistic{" +
                "serverId=" + serverId +
                ", emojiId=" + emojiId +
                ", usagesCount=" + usagesCount +
                ", lastUsage=" + lastUsage +
                '}';
    }
}

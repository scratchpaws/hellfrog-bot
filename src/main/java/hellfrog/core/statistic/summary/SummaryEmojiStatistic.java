package hellfrog.core.statistic.summary;

import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class SummaryEmojiStatistic
        implements Comparable<SummaryEmojiStatistic> {

    private KnownCustomEmoji emoji;
    private long usagesCount;
    private Instant lastUsage;

    public KnownCustomEmoji getEmoji() {
        return emoji;
    }

    public void setEmoji(KnownCustomEmoji emoji) {
        this.emoji = emoji;
    }

    public long getUsagesCount() {
        return usagesCount;
    }

    public void setUsagesCount(long usagesCount) {
        this.usagesCount = usagesCount;
    }

    public Instant getLastUsage() {
        return lastUsage;
    }

    public void setLastUsage(Instant lastUsage) {
        this.lastUsage = lastUsage;
    }

    @Override
    public int compareTo(@NotNull SummaryEmojiStatistic o) {
        if (usagesCount > o.usagesCount) {
            return 1;
        } else if (usagesCount < o.usagesCount) {
            return -1;
        }
        if (emoji == null || o.emoji == null) {
            return 0;
        }
        return emoji.getName().compareTo(o.emoji.getName());
    }
}

package hellfrog.settings.oldjson;

import java.util.concurrent.atomic.AtomicLong;

// hellfrog.settings.db.entity.EmojiTotalStatistic
public class JSONSmileStatistic {

    private AtomicLong usagesCount = new AtomicLong(0L);
    private AtomicLong lastUsage = new AtomicLong(-1L);

    public AtomicLong getUsagesCount() {
        return usagesCount;
    }

    public void setUsagesCount(AtomicLong usagesCount) {
        this.usagesCount = usagesCount != null ? usagesCount : this.usagesCount;
    }

    public AtomicLong getLastUsage() {
        return lastUsage;
    }

    public void setLastUsage(AtomicLong lastUsage) {
        this.lastUsage = lastUsage != null ? lastUsage : this.lastUsage;
    }
}

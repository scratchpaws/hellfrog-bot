package hellfrog.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hellfrog.common.CommonUtils;

import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Deprecated
public class SmileStatistic {

    @JsonIgnore
    private final ReentrantLock creationLock = new ReentrantLock();
    private volatile AtomicLong usagesCount = new AtomicLong(0L);
    private volatile AtomicLong lastUsage = new AtomicLong(-1L);

    @Deprecated
    public AtomicLong getUsagesCount() {
        return usagesCount;
    }

    @Deprecated
    public void setUsagesCount(AtomicLong usagesCount) {
        this.usagesCount = usagesCount;
    }

    @Deprecated
    public AtomicLong getLastUsage() {
        return lastUsage;
    }

    @Deprecated
    public void setLastUsage(AtomicLong lastUsage) {
        this.lastUsage = lastUsage;
    }

    @Deprecated
    public void increment() {
        checkExist();
        usagesCount.incrementAndGet();
        updateLastUsage();
    }

    @Deprecated
    public void decrement() {
        checkExist();
        if (usagesCount.get() > 0)
            usagesCount.decrementAndGet();
        updateLastUsage();
    }

    @Deprecated
    public void incrementWithLastDate(Instant lastDate) {
        checkExist();
        usagesCount.incrementAndGet();
        lastUsage.set(CommonUtils.getLatestDate(lastDate, lastUsage.get()));
    }

    @Deprecated
    private void updateLastUsage() {
        if (lastUsage == null) return;
        Calendar current = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUsage.set(current.getTimeInMillis());
    }

    @Deprecated
    private void checkExist() {
        if (usagesCount == null || lastUsage == null) {
            creationLock.lock();
            try {
                if (usagesCount == null)
                    usagesCount = new AtomicLong(0L);
                if (lastUsage == null)
                    lastUsage = new AtomicLong(-1L);
            } finally {
                creationLock.unlock();
            }
        }
    }
}

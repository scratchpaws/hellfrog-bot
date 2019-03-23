package xyz.funforge.scratchypaws.hellfrog.settings.old;

import com.fasterxml.jackson.annotation.JsonIgnore;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;

import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class SmileStatistic {

    private volatile AtomicLong usagesCount = new AtomicLong(0L);
    private volatile AtomicLong lastUsage = new AtomicLong(-1L);
    @JsonIgnore
    private ReentrantLock creationLock = new ReentrantLock();

    public AtomicLong getUsagesCount() {
        return usagesCount;
    }

    public AtomicLong getLastUsage() {
        return lastUsage;
    }

    public void setUsagesCount(AtomicLong usagesCount) {
        this.usagesCount = usagesCount;
    }

    public void setLastUsage(AtomicLong lastUsage) {
        this.lastUsage = lastUsage;
    }

    public void increment() {
        checkExist();
        usagesCount.incrementAndGet();
        updateLastUsage();
    }

    public void decrement() {
        checkExist();
        if (usagesCount.get() > 0)
            usagesCount.decrementAndGet();
        updateLastUsage();
    }

    public void incrementWithLastDate(Instant lastDate) {
        checkExist();
        usagesCount.incrementAndGet();
        lastUsage.set(CommonUtils.getLatestDate(lastDate, lastUsage.get()));
    }

    private void updateLastUsage() {
        if (lastUsage == null) return;
        Calendar current = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        lastUsage.set(current.getTimeInMillis());
    }

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

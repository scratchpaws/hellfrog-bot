package xyz.funforge.scratchypaws.hellfrog.settings.old;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ServerStatistic {

    private volatile Boolean collectNonDefaultSmileStats = Boolean.FALSE;
    private volatile ConcurrentHashMap<Long, SmileStatistic> nonDefaultSmileStats = new ConcurrentHashMap<>();
    @JsonIgnore
    private ReentrantLock createSmileStatLock = new ReentrantLock();

    public boolean isCollectNonDefaultSmileStats() {
        return collectNonDefaultSmileStats != null && collectNonDefaultSmileStats;
    }

    public void setCollectNonDefaultSmileStats(boolean collectNonDefaultSmileStats) {
        this.collectNonDefaultSmileStats = collectNonDefaultSmileStats;
    }

    public ConcurrentHashMap<Long, SmileStatistic> getNonDefaultSmileStats() {
        return nonDefaultSmileStats;
    }

    public void setNonDefaultSmileStats(ConcurrentHashMap<Long, SmileStatistic> nonDefaultSmileStats) {
        this.nonDefaultSmileStats = nonDefaultSmileStats;
    }

    public SmileStatistic getSmileStatistic(long emojiId) {
        if (nonDefaultSmileStats.containsKey(emojiId)) {
            return nonDefaultSmileStats.get(emojiId);
        } else {
            createSmileStatLock.lock();
            try {
                if (!nonDefaultSmileStats.containsKey(emojiId)) {
                    SmileStatistic smileStatistic = new SmileStatistic();
                    nonDefaultSmileStats.put(emojiId, smileStatistic);
                }
                return nonDefaultSmileStats.get(emojiId);
            } finally {
                createSmileStatLock.unlock();
            }
        }
    }
}

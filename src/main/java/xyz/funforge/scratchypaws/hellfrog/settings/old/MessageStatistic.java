package xyz.funforge.scratchypaws.hellfrog.settings.old;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class MessageStatistic {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private volatile AtomicLong messagesCount = new AtomicLong(0L);
    private volatile AtomicLong lastMessageDate = new AtomicLong(-1L);
    private long entityId = 0L;
    private volatile String lastKnownName = "";
    private volatile ConcurrentHashMap<Long, MessageStatistic> childStatistic = null;

    @JsonIgnore
    private ReentrantLock childCreationLock = new ReentrantLock();

    public AtomicLong getMessagesCount() {
        return messagesCount;
    }

    public void setMessagesCount(AtomicLong messagesCount) {
        this.messagesCount = messagesCount;
    }

    public AtomicLong getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(AtomicLong lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    @JsonIgnore
    public long getCount() {
        if (messagesCount != null)
            return messagesCount.get();
        return 0L;
    }

    @JsonIgnore
    public String getLastDate(@NotNull TimeZone timeZone) {
        if (lastMessageDate != null && lastMessageDate.get() > 0L) {
            long value = lastMessageDate.get();
            Calendar result = Calendar.getInstance(timeZone);
            result.setTimeInMillis(value);
            return String.format("%tF %<tT (%s)", result, timeZone.getID());
        }

        return "";
    }

    @JsonIgnore
    public String getLastDate() {
        return getLastDate(UTC);
    }

    public void increment() {
        updateLastMessage();
        messagesCount.incrementAndGet();
    }

    public void decrement() {
        if (messagesCount.get() > 0L) {
            messagesCount.decrementAndGet();
        }
    }

    private void updateLastMessage() {
        Calendar value = Calendar.getInstance(UTC);
        lastMessageDate.set(value.getTimeInMillis());
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageStatistic that = (MessageStatistic) o;
        return entityId == that.entityId &&
                Objects.equals(messagesCount, that.messagesCount) &&
                Objects.equals(lastMessageDate, that.lastMessageDate) &&
                Objects.equals(lastKnownName, that.lastKnownName) &&
                Objects.equals(childStatistic, that.childStatistic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messagesCount, lastMessageDate, entityId, lastKnownName, childStatistic);
    }

    public ConcurrentHashMap<Long, MessageStatistic> getChildStatistic() {
        return childStatistic;
    }

    public void setChildStatistic(ConcurrentHashMap<Long, MessageStatistic> childStatistic) {
        this.childStatistic = childStatistic;
    }

    @JsonIgnore
    public MessageStatistic getChildItemStatistic(long itemId) {
        if (childStatistic == null) {
            childCreationLock.lock();
            try {
                if (childStatistic == null) {
                    childStatistic = new ConcurrentHashMap<>();
                }
            } finally {
                childCreationLock.unlock();
            }
        }
        if (!childStatistic.containsKey(itemId)) {
            childCreationLock.lock();
            try {
                if (!childStatistic.containsKey(itemId)) {
                    MessageStatistic stat = new MessageStatistic();
                    stat.setEntityId(itemId);
                    childStatistic.put(itemId, stat);
                }
            } finally {
                childCreationLock.unlock();
            }
        }
        return childStatistic.get(itemId);
    }
}

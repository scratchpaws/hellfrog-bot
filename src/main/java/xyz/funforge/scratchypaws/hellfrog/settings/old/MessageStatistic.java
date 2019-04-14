package xyz.funforge.scratchypaws.hellfrog.settings.old;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;

import java.time.Instant;
import java.util.Calendar;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class MessageStatistic {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    @JsonIgnore
    private final ReentrantLock childCreationLock = new ReentrantLock();
    private volatile AtomicLong messagesCount = new AtomicLong(0L);
    private volatile AtomicLong lastMessageDate = new AtomicLong(-1L);
    private volatile AtomicLong totalSymbolsCount = new AtomicLong(0L);
    private volatile AtomicLong totalBytesCount = new AtomicLong(0L);
    private long entityId = 0L;
    private volatile String lastKnownName = "";
    private volatile ConcurrentHashMap<Long, MessageStatistic> childStatistic = null;

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

    public AtomicLong getTotalSymbolsCount() {
        return totalSymbolsCount;
    }

    public void setTotalSymbolsCount(AtomicLong totalSymbolsCount) {
        this.totalSymbolsCount = totalSymbolsCount;
    }

    public AtomicLong getTotalBytesCount() {
        return totalBytesCount;
    }

    public void setTotalBytesCount(AtomicLong totalBytesCount) {
        this.totalBytesCount = totalBytesCount;
    }

    @JsonIgnore
    public long getCountOfMessages() {
        if (messagesCount != null)
            return messagesCount.get();
        return 0L;
    }

    @JsonIgnore
    public long getCountOfSymbols() {
        if (totalSymbolsCount != null) {
            long _totalSymbs = totalSymbolsCount.get();
            return _totalSymbs >= 0 ? _totalSymbs : 0L;
        }
        return 0L;
    }

    @JsonIgnore
    public long getCountOfBytes() {
        if (totalBytesCount != null) {
            long _totalBytes = totalBytesCount.get();
            return _totalBytes >= 0 ? _totalBytes : 0L;
        }
        return 0L;
    }

    @JsonIgnore
    public long getSummaryCount() {
        long messages = this.getCountOfMessages(); // при нуле сообщений не учитываем остальное
        long symbols = getOneIfZero(this.getCountOfSymbols());
        long bytes = getOneIfZero(this.getCountOfBytes() / 1048576L); // 1 Мб
        return messages * symbols * bytes;
    }

    @JsonIgnore
    private long getOneIfZero(long value) {
        return value > 0 ? value : 1L;
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

    public void increment(int messageLength, long bytesCount) {
        updateLastMessage();
        messagesCount.incrementAndGet();
        totalSymbolsCount.addAndGet(messageLength);
        totalBytesCount.addAndGet(bytesCount);
    }

    public void incrementWithLastDate(Instant lastDate, int messageLength, long bytesCount) {
        messagesCount.incrementAndGet();
        totalSymbolsCount.addAndGet(messageLength);
        totalBytesCount.addAndGet(bytesCount);
        lastMessageDate.set(CommonUtils.getLatestDate(lastDate, lastMessageDate.get()));
    }

    public void decrement(int messageLength, long bytesCount) {
        if (messagesCount.get() > 0L) {
            messagesCount.decrementAndGet();
        }
        if (totalSymbolsCount.get() >= messageLength) {
            totalSymbolsCount.addAndGet(messageLength * (-1));
        }
        if (totalBytesCount.get() >= bytesCount) {
            totalBytesCount.addAndGet(bytesCount * (-1));
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

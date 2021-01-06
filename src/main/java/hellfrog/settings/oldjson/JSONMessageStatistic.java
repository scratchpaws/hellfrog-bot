package hellfrog.settings.oldjson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

// hellfrog.settings.db.entity.TextChannelTotalStatistic
public class JSONMessageStatistic {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private AtomicLong messagesCount = new AtomicLong(0L);
    private AtomicLong lastMessageDate = new AtomicLong(-1L);
    private AtomicLong totalSymbolsCount = new AtomicLong(0L);
    private AtomicLong totalBytesCount = new AtomicLong(0L);
    private long entityId = 0L;
    private String lastKnownName = "";
    private Map<Long, JSONMessageStatistic> childStatistic = null;

    public AtomicLong getMessagesCount() {
        return messagesCount;
    }

    public void setMessagesCount(AtomicLong messagesCount) {
        this.messagesCount = messagesCount != null ? messagesCount : this.messagesCount;
    }

    public AtomicLong getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(AtomicLong lastMessageDate) {
        this.lastMessageDate = lastMessageDate != null ? lastMessageDate : this.lastMessageDate;
    }

    public AtomicLong getTotalSymbolsCount() {
        return totalSymbolsCount;
    }

    public void setTotalSymbolsCount(AtomicLong totalSymbolsCount) {
        this.totalSymbolsCount = totalSymbolsCount != null ? totalSymbolsCount : this.totalSymbolsCount;
    }

    public AtomicLong getTotalBytesCount() {
        return totalBytesCount;
    }

    public void setTotalBytesCount(AtomicLong totalBytesCount) {
        this.totalBytesCount = totalBytesCount != null ? totalBytesCount : this.totalBytesCount;
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

    @Contract(pure = true)
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

    public Map<Long, JSONMessageStatistic> getChildStatistic() {
        return childStatistic;
    }

    public void setChildStatistic(Map<Long, JSONMessageStatistic> childStatistic) {
        this.childStatistic = childStatistic != null ? Collections.unmodifiableMap(childStatistic) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSONMessageStatistic that = (JSONMessageStatistic) o;
        return entityId == that.entityId &&
                Objects.equals(messagesCount, that.messagesCount) &&
                Objects.equals(lastMessageDate, that.lastMessageDate) &&
                Objects.equals(totalSymbolsCount, that.totalSymbolsCount) &&
                Objects.equals(totalBytesCount, that.totalBytesCount) &&
                Objects.equals(lastKnownName, that.lastKnownName) &&
                Objects.equals(childStatistic, that.childStatistic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messagesCount, lastMessageDate, totalSymbolsCount,
                totalBytesCount, entityId, lastKnownName, childStatistic);
    }
}

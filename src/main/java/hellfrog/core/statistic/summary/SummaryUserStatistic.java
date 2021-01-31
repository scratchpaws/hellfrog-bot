package hellfrog.core.statistic.summary;

import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Objects;

public class SummaryUserStatistic
        implements Comparable<SummaryUserStatistic> {

    private static final long MEGABYTE = 1_048_576L;

    private User user;
    private boolean userPresent;
    private String lastKnownNick;
    private String discriminationName;
    private Instant lastMessageDate;
    private long userId;
    private long messagesCount;
    private long symbolsCount;
    private long bytesCount;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isUserPresent() {
        return userPresent;
    }

    public void setUserPresent(boolean userPresent) {
        this.userPresent = userPresent;
    }

    public String getLastKnownNick() {
        return lastKnownNick;
    }

    public void setLastKnownNick(String lastKnownNick) {
        this.lastKnownNick = lastKnownNick;
    }

    public String getDiscriminationName() {
        return discriminationName;
    }

    public void setDiscriminationName(String discriminationName) {
        this.discriminationName = discriminationName;
    }

    public Instant getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(Instant lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getSummaryCount() {
        // If there are no messages, the rest of the statistics is discarded
        return this.messagesCount * getOneIfZero(this.symbolsCount) * getOneIfZero(this.bytesCount / MEGABYTE);
    }

    public long getMessagesCount() {
        return messagesCount;
    }

    public void setMessagesCount(long messagesCount) {
        this.messagesCount = messagesCount;
    }

    public long getSymbolsCount() {
        return symbolsCount;
    }

    public void setSymbolsCount(long symbolsCount) {
        this.symbolsCount = symbolsCount;
    }

    public long getBytesCount() {
        return bytesCount;
    }

    public void setBytesCount(long bytesCount) {
        this.bytesCount = bytesCount;
    }

    private long getOneIfZero(long value) {
        return value > 0 ? value : 1L;
    }

    @Override
    public int compareTo(@NotNull SummaryUserStatistic o) {
        if (this.getSummaryCount() > o.getSummaryCount()) {
            return 1;
        } else if (this.getSummaryCount() < o.getSummaryCount()) {
            return -1;
        }
        if (lastKnownNick != null && o.lastKnownNick != null) {
            return lastKnownNick.compareTo(o.lastKnownNick);
        }
        if (discriminationName != null && o.discriminationName != null) {
            return discriminationName.compareTo(o.discriminationName);
        }
        if (userId > o.userId) {
            return 1;
        } else if (userId < o.userId) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SummaryUserStatistic that = (SummaryUserStatistic) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "SummaryUserStatistic{" +
                "user=" + user +
                ", userPresent=" + userPresent +
                ", lastKnownNick='" + lastKnownNick + '\'' +
                ", discriminationName='" + discriminationName + '\'' +
                ", lastMessageDate=" + lastMessageDate +
                ", userId=" + userId +
                ", messagesCount=" + messagesCount +
                ", symbolsCount=" + symbolsCount +
                ", bytesCount=" + bytesCount +
                '}';
    }
}

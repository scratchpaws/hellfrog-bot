package hellfrog.core.statistic.summary;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;

public class SummaryChannelStatistic
        implements Comparable<SummaryChannelStatistic> {

    private static final long MEGABYTE = 1_048_576L;

    private ServerTextChannel textChannel;
    private boolean textChannelPresent;
    private String lastKnownName;
    private long channelId;

    private final TreeSet<SummaryUserStatistic> summaryUserStatistics = new TreeSet<>(Comparator.reverseOrder());

    public ServerTextChannel getTextChannel() {
        return textChannel;
    }

    public void setTextChannel(ServerTextChannel textChannel) {
        this.textChannel = textChannel;
    }

    public boolean isTextChannelPresent() {
        return textChannelPresent;
    }

    public void setTextChannelPresent(boolean textChannelPresent) {
        this.textChannelPresent = textChannelPresent;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public Instant getLastMessageDate() {
        Instant lastDate = null;
        for (SummaryUserStatistic userStatistic : summaryUserStatistics) {
            if (lastDate == null || (userStatistic.getLastMessageDate() != null
                    && userStatistic.getLastMessageDate().isAfter(lastDate))) {
                lastDate = userStatistic.getLastMessageDate();
            }
        }
        return lastDate;
    }

    public long getSummaryCount() {
        // If there are no messages, the rest of the statistics is discarded
        return this.getMessagesCount() * getOneIfZero(this.getSymbolsCount()) * getOneIfZero(this.getBytesCount() / MEGABYTE);
    }

    private long getOneIfZero(long value) {
        return value > 0 ? value : 1L;
    }

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    public long getMessagesCount() {
        return summaryUserStatistics.stream()
                .mapToLong(SummaryUserStatistic::getMessagesCount)
                .sum();
    }

    public long getSymbolsCount() {
        return summaryUserStatistics.stream()
                .mapToLong(SummaryUserStatistic::getSymbolsCount)
                .sum();
    }

    public long getBytesCount() {
        return summaryUserStatistics.stream()
                .mapToLong(SummaryUserStatistic::getBytesCount)
                .sum();
    }

    public TreeSet<SummaryUserStatistic> getSummaryUserStatistics() {
        return summaryUserStatistics;
    }

    @Override
    public int compareTo(@NotNull SummaryChannelStatistic o) {
        if (this.getSummaryCount() > o.getSummaryCount()) {
            return 1;
        } else if (this.getSummaryCount() < o.getSummaryCount()) {
            return -1;
        }
        if (lastKnownName != null && o.lastKnownName != null) {
            return lastKnownName.compareTo(o.lastKnownName);
        }
        if (channelId > o.channelId) {
            return 1;
        } else if (channelId < o.channelId) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SummaryChannelStatistic that = (SummaryChannelStatistic) o;
        return channelId == that.channelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId);
    }

    @Override
    public String toString() {
        return "SummaryChannelStatistic{" +
                "textChannel=" + textChannel +
                ", textChannelPresent=" + textChannelPresent +
                ", lastKnownName='" + lastKnownName + '\'' +
                ", channelId=" + channelId +
                ", summaryUserStatistics=" + summaryUserStatistics +
                '}';
    }
}

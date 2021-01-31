package hellfrog.core.statistic.summary;

import org.javacord.api.entity.emoji.KnownCustomEmoji;

import java.time.Instant;
import java.util.TreeSet;

public class SummaryReport {

    private TreeSet<KnownCustomEmoji> nonUsedEmoji;
    private TreeSet<SummaryEmojiStatistic> emojiSummaryStatistic;
    private TreeSet<SummaryUserStatistic> summaryUserStatistics;
    private TreeSet<SummaryChannelStatistic> summaryChannelStatistics;
    private Instant sinceDate;
    private boolean hasSinceDate;

    public TreeSet<KnownCustomEmoji> getNonUsedEmoji() {
        return nonUsedEmoji;
    }

    public void setNonUsedEmoji(TreeSet<KnownCustomEmoji> nonUsedEmoji) {
        this.nonUsedEmoji = nonUsedEmoji;
    }

    public TreeSet<SummaryEmojiStatistic> getEmojiSummaryStatistic() {
        return emojiSummaryStatistic;
    }

    public void setEmojiSummaryStatistic(TreeSet<SummaryEmojiStatistic> emojiSummaryStatistic) {
        this.emojiSummaryStatistic = emojiSummaryStatistic;
    }

    public TreeSet<SummaryUserStatistic> getSummaryUserStatistics() {
        return summaryUserStatistics;
    }

    public void setSummaryUserStatistics(TreeSet<SummaryUserStatistic> summaryUserStatistics) {
        this.summaryUserStatistics = summaryUserStatistics;
    }

    public TreeSet<SummaryChannelStatistic> getSummaryChannelStatistics() {
        return summaryChannelStatistics;
    }

    public void setSummaryChannelStatistics(TreeSet<SummaryChannelStatistic> summaryChannelStatistics) {
        this.summaryChannelStatistics = summaryChannelStatistics;
    }

    public Instant getSinceDate() {
        return sinceDate;
    }

    public void setSinceDate(Instant sinceDate) {
        this.sinceDate = sinceDate;
    }

    public boolean isHasSinceDate() {
        return hasSinceDate;
    }

    public void setHasSinceDate(boolean hasSinceDate) {
        this.hasSinceDate = hasSinceDate;
    }

    public boolean isEmpty() {
        return (nonUsedEmoji == null || nonUsedEmoji.isEmpty())
                && (emojiSummaryStatistic == null || emojiSummaryStatistic.isEmpty())
                && (summaryUserStatistics == null || summaryUserStatistics.isEmpty())
                && (summaryChannelStatistics == null || summaryChannelStatistics.isEmpty());
    }

    @Override
    public String toString() {
        return "SummaryReport{" +
                "nonUsedEmoji=" + nonUsedEmoji +
                ", emojiSummaryStatistic=" + emojiSummaryStatistic +
                ", summaryUserStatistics=" + summaryUserStatistics +
                ", summaryChannelStatistics=" + summaryChannelStatistics +
                ", sinceDate=" + sinceDate +
                ", hasSinceDate=" + hasSinceDate +
                '}';
    }
}

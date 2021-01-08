package hellfrog.settings;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Активное голосование
 */
@Deprecated
public class ActiveVote
        implements Serializable {

    private short id;
    private boolean hasTimer;
    private long endDate;
    private List<Long> rolesFilter;
    private Long textChatId;
    private Long messageId;
    private List<VotePoint> votePoints;
    private String readableVoteText;
    private Boolean exceptionalVote;
    private Boolean withDefaultPoint;
    private Long winThreshold;

    @Deprecated
    public short getId() {
        return id;
    }

    @Deprecated
    public void setId(short id) {
        this.id = id;
    }

    @Deprecated
    public boolean isHasTimer() {
        return hasTimer;
    }

    @Deprecated
    public void setHasTimer(boolean hasTimer) {
        this.hasTimer = hasTimer;
    }

    @Deprecated
    public long getEndDate() {
        return endDate;
    }

    @Deprecated
    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    @Deprecated
    public List<Long> getRolesFilter() {
        return rolesFilter;
    }

    @Deprecated
    public void setRolesFilter(List<Long> rolesFilter) {
        this.rolesFilter = rolesFilter;
    }

    @Deprecated
    public Long getTextChatId() {
        return textChatId;
    }

    @Deprecated
    public void setTextChatId(Long textChatId) {
        this.textChatId = textChatId;
    }

    @Deprecated
    public Long getMessageId() {
        return messageId;
    }

    @Deprecated
    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    @Deprecated
    public List<VotePoint> getVotePoints() {
        return votePoints;
    }

    @Deprecated
    public void setVotePoints(List<VotePoint> votePoints) {
        this.votePoints = votePoints;
    }

    @Deprecated
    public String getReadableVoteText() {
        return readableVoteText;
    }

    @Deprecated
    public void setReadableVoteText(String readableVoteText) {
        this.readableVoteText = readableVoteText;
    }

    @Deprecated
    public boolean isExceptionalVote() {
        return exceptionalVote != null && exceptionalVote;
    }

    @Deprecated
    public void setExceptionalVote(Boolean exceptionalVote) {
        this.exceptionalVote = exceptionalVote != null ? exceptionalVote : false;
    }

    @Deprecated
    public boolean isWithDefaultPoint() {
        return withDefaultPoint != null && withDefaultPoint;
    }

    @Deprecated
    public void setWithDefaultPoint(boolean withDefaultPoint) {
        this.withDefaultPoint = withDefaultPoint;
    }

    @Deprecated
    public long getWinThreshold() {
        return this.winThreshold != null ? this.winThreshold : -1L;
    }

    @Deprecated
    public void setWinThreshold(long winThreshold) {
        this.winThreshold = winThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveVote that = (ActiveVote) o;
        return id == that.id &&
                hasTimer == that.hasTimer &&
                endDate == that.endDate &&
                Objects.equals(rolesFilter, that.rolesFilter) &&
                Objects.equals(textChatId, that.textChatId) &&
                Objects.equals(messageId, that.messageId) &&
                Objects.equals(votePoints, that.votePoints) &&
                Objects.equals(readableVoteText, that.readableVoteText) &&
                Objects.equals(exceptionalVote, that.exceptionalVote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hasTimer, endDate, rolesFilter, textChatId,
                messageId, votePoints, readableVoteText, exceptionalVote);
    }

    @Override
    public String toString() {
        return "ActiveVote{" +
                "id=" + id +
                ",\n hasTimer=" + hasTimer +
                ",\n endDate=" + endDate +
                ",\n rolesFilter=" + rolesFilter +
                ",\n textChatId=" + textChatId +
                ",\n messageId=" + messageId +
                ",\n votePoints=" + votePoints +
                ",\n readableVoteText='" + readableVoteText + '\'' +
                ",\n exceptionalVote=" + exceptionalVote +
                '}';
    }
}

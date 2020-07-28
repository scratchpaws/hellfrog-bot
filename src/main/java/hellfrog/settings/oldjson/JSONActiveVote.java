package hellfrog.settings.oldjson;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

// hellfrog.settings.db.entity.Vote
public class JSONActiveVote {

    // hellfrog.settings.db.entity.Vote.id
    private short id;
    // hellfrog.settings.db.entity.Vote.hasTimer
    private boolean hasTimer;
    // hellfrog.settings.db.entity.Vote.finishTime
    private long endDate;
    // hellfrog.settings.db.entity.Vote.rolesFilter
    private List<Long> rolesFilter;
    // hellfrog.settings.db.entity.Vote.textChatId
    private Long textChatId;
    // hellfrog.settings.db.entity.Vote.messageId
    private Long messageId;
    // hellfrog.settings.db.entity.Vote.votePoints
    private List<JSONVotePoint> votePoints;
    // hellfrog.settings.db.entity.Vote.voteText
    private String readableVoteText;
    // hellfrog.settings.db.entity.Vote.isExceptional
    private boolean exceptionalVote;
    // hellfrog.settings.db.entity.Vote.hasDefault
    private boolean withDefaultPoint;
    // hellfrog.settings.db.entity.Vote.winThreshold
    private long winThreshold;

    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public boolean isHasTimer() {
        return hasTimer;
    }

    public void setHasTimer(boolean hasTimer) {
        this.hasTimer = hasTimer;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public List<Long> getRolesFilter() {
        return rolesFilter;
    }

    public void setRolesFilter(List<Long> rolesFilter) {
        this.rolesFilter = rolesFilter != null ? Collections.unmodifiableList(rolesFilter) : null;
    }

    public Long getTextChatId() {
        return textChatId;
    }

    public void setTextChatId(Long textChatId) {
        this.textChatId = textChatId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public List<JSONVotePoint> getVotePoints() {
        return votePoints;
    }

    public void setVotePoints(List<JSONVotePoint> votePoints) {
        this.votePoints = votePoints != null ? Collections.unmodifiableList(votePoints) : null;
    }

    public String getReadableVoteText() {
        return readableVoteText;
    }

    public void setReadableVoteText(String readableVoteText) {
        this.readableVoteText = readableVoteText;
    }

    public boolean isExceptionalVote() {
        return exceptionalVote;
    }

    public void setExceptionalVote(Boolean exceptionalVote) {
        this.exceptionalVote = exceptionalVote != null ? exceptionalVote : false;
    }

    public boolean isWithDefaultPoint() {
        return withDefaultPoint;
    }

    public void setWithDefaultPoint(boolean withDefaultPoint) {
        this.withDefaultPoint = withDefaultPoint;
    }

    public long getWinThreshold() {
        return winThreshold;
    }

    public void setWinThreshold(long winThreshold) {
        this.winThreshold = winThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSONActiveVote that = (JSONActiveVote) o;
        return id == that.id &&
                hasTimer == that.hasTimer &&
                endDate == that.endDate &&
                Objects.equals(rolesFilter, that.rolesFilter) &&
                Objects.equals(textChatId, that.textChatId) &&
                Objects.equals(messageId, that.messageId) &&
                Objects.equals(votePoints, that.votePoints) &&
                Objects.equals(readableVoteText, that.readableVoteText) &&
                Objects.equals(exceptionalVote, that.exceptionalVote) &&
                Objects.equals(withDefaultPoint, that.withDefaultPoint) &&
                Objects.equals(winThreshold, that.winThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, hasTimer, endDate, rolesFilter, textChatId, messageId,
                votePoints, readableVoteText, exceptionalVote, withDefaultPoint, winThreshold);
    }

    @Override
    public String toString() {
        return "ActiveVote{" +
                "id=" + id +
                ", hasTimer=" + hasTimer +
                ", endDate=" + endDate +
                ", rolesFilter=" + rolesFilter +
                ", textChatId=" + textChatId +
                ", messageId=" + messageId +
                ", votePoints=" + votePoints +
                ", readableVoteText='" + readableVoteText + '\'' +
                ", exceptionalVote=" + exceptionalVote +
                ", withDefaultPoint=" + withDefaultPoint +
                ", winThreshold=" + winThreshold +
                '}';
    }
}

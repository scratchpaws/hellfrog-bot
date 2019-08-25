package hellfrog.settings.oldjson;

import hellfrog.settings.VotePoint;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JSONActiveVote {

    private short id; // vote_id
    private boolean hasTimer;
    private long endDate;
    private List<Long> rolesFilter;
    private Long textChatId;
    private Long messageId;
    private List<VotePoint> votePoints;
    private String readableVoteText;
    private boolean exceptionalVote;
    private boolean withDefaultPoint;
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

    public List<VotePoint> getVotePoints() {
        return votePoints;
    }

    public void setVotePoints(List<VotePoint> votePoints) {
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
}

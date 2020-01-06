package hellfrog.settings.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Vote {

    private long id = -1L;
    private long serverId = 0L;
    private long textChatId = 0L;
    private long messageId = 0L;
    private boolean isActive = false;
    private Instant finishTime = null;
    private String voteText = null;
    private boolean hasTimer = false;
    private boolean isExceptional = false;
    private boolean hasDefault = false;
    private long winThreshold = 0L;
    private Instant createDate = null;
    private Instant updateDate = null;
    private List<VotePoint> votePoints = new ArrayList<>();

    public Vote() {}

    public long getId() {
        return id;
    }

    public long getServerId() {
        return serverId;
    }

    public long getTextChatId() {
        return textChatId;
    }

    public long getMessageId() {
        return messageId;
    }

    public boolean isActive() {
        return isActive;
    }

    public Optional<Instant> getFinishTime() {
        return Optional.ofNullable(finishTime);
    }

    public Optional<String> getVoteText() {
        return Optional.ofNullable(voteText);
    }

    public boolean isHasTimer() {
        return hasTimer;
    }

    public boolean isExceptional() {
        return isExceptional;
    }

    public boolean isHasDefault() {
        return hasDefault;
    }

    public long getWinThreshold() {
        return winThreshold;
    }

    public Optional<Instant> getCreateDate() {
        return Optional.ofNullable(createDate);
    }

    public Optional<Instant> getUpdateDate() {
        return Optional.ofNullable(updateDate);
    }

    public Vote setId(long id) {
        this.id = id;
        return this;
    }

    public Vote setServerId(long serverId) {
        this.serverId = serverId;
        return this;
    }

    public Vote setTextChatId(long textChatId) {
        this.textChatId = textChatId;
        return this;
    }

    public Vote setMessageId(long messageId) {
        this.messageId = messageId;
        return this;
    }

    public Vote setActive(boolean active) {
        isActive = active;
        return this;
    }

    public Vote setFinishTime(Instant finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public Vote setVoteText(String voteText) {
        this.voteText = voteText;
        return this;
    }

    public Vote setHasTimer(boolean hasTimer) {
        this.hasTimer = hasTimer;
        return this;
    }

    public Vote setExceptional(boolean exceptional) {
        isExceptional = exceptional;
        return this;
    }

    public Vote setHasDefault(boolean hasDefault) {
        this.hasDefault = hasDefault;
        return this;
    }

    public Vote setWinThreshold(long winThreshold) {
        this.winThreshold = winThreshold;
        return this;
    }

    public Vote setCreateDate(Instant createDate) {
        this.createDate = createDate;
        return this;
    }

    public Vote setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public List<VotePoint> getVotePoints() {
        return votePoints != null ? votePoints : new ArrayList<>();
    }

    public Vote setVotePoints(List<VotePoint> votePoints) {
        this.votePoints = votePoints != null ? votePoints : new ArrayList<>();
        return this;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", textChatId=" + textChatId +
                ", messageId=" + messageId +
                ", isActive=" + isActive +
                ", finishTime=" + finishTime +
                ", voteText='" + voteText + '\'' +
                ", hasTimer=" + hasTimer +
                ", isExceptional=" + isExceptional +
                ", hasDefault=" + hasDefault +
                ", winThreshold=" + winThreshold +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                ", votePoints=" + votePoints +
                '}';
    }
}

package hellfrog.settings.entity;

import hellfrog.common.CommonUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Vote {

    public enum Columns {

        ID(1, -1),
        SERVER_ID(2, 1),
        TEXT_CHAT_ID(3, 2),
        MESSAGE_ID(4, 3),
        FINISH_DATE(5, 4),
        VOTE_TEXT(6, 5),
        HAS_TIMER(7, 6),
        IS_EXCEPTIONAL(8, 7),
        HAS_DEFAULT(9, 8),
        WIN_THRESHOLD(10, 9),
        CREATE_DATE(11, 10),
        UPDATE_DATE(12, 11);

        public final int selectColumn;
        public final int insertColumn;

        Columns(int selectColumn, int insertColumn) {
            this.selectColumn = selectColumn;
            this.insertColumn = insertColumn;
        }
    }

    private long id = -1L;
    private long serverId = 0L;
    private long textChatId = 0L;
    private long messageId = 0L;
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

    public Vote setFinishTime(Instant finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public Vote setVoteText(String voteText) {
        this.voteText = CommonUtils.isTrStringNotEmpty(voteText) ? voteText : null;
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

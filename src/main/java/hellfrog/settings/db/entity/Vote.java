package hellfrog.settings.db.entity;

import hellfrog.common.CommonUtils;
import hellfrog.settings.db.h2.BooleanToLongConverter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "active_votes")
public class Vote {

    private long id;
    private long serverId;
    private long textChatId;
    private long messageId;
    private Timestamp finishTime;
    private String voteText;
    private boolean hasTimer;
    private boolean isExceptional;
    private boolean hasDefault;
    private long winThreshold;
    private Timestamp createDate;
    private Timestamp updateDate;
    private Set<VotePoint> votePoints;
    private Set<VoteRoleFilter> rolesFilter;
    public Vote() {
    }

    @PrePersist
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
        if (updateDate == null) {
            updateDate = Timestamp.from(Instant.now());
        }
        if (CommonUtils.isTrStringEmpty(voteText)) {
            voteText = " ";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateDate = Timestamp.from(Instant.now());
    }

    @Id
    @GeneratedValue(generator = "active_vote_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "active_vote_ids", sequenceName = "active_vote_ids")
    @Column(name = "id", nullable = false, unique = true)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name = "server_id", nullable = false)
    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    @Column(name = "text_chat_id", nullable = false)
    public long getTextChatId() {
        return textChatId;
    }

    public void setTextChatId(long textChatId) {
        this.textChatId = textChatId;
    }

    @Column(name = "message_id", nullable = false)
    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    @Column(name = "finish_date")
    public Timestamp getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Timestamp finishTime) {
        this.finishTime = finishTime;
    }

    @Column(name = "vote_text")
    public String getVoteText() {
        return voteText;
    }

    public void setVoteText(String voteText) {
        this.voteText = voteText;
    }

    @Column(name = "has_timer", nullable = false)
    @Convert(converter = BooleanToLongConverter.class)
    public boolean isHasTimer() {
        return hasTimer;
    }

    public void setHasTimer(boolean hasTimer) {
        this.hasTimer = hasTimer;
    }

    @Column(name = "is_exceptional", nullable = false)
    @Convert(converter = BooleanToLongConverter.class)
    public boolean isExceptional() {
        return isExceptional;
    }

    public void setExceptional(boolean exceptional) {
        isExceptional = exceptional;
    }

    @Column(name = "has_default", nullable = false)
    @Convert(converter = BooleanToLongConverter.class)
    public boolean isHasDefault() {
        return hasDefault;
    }

    public void setHasDefault(boolean hasDefault) {
        this.hasDefault = hasDefault;
    }

    @Column(name = "win_threshold", nullable = false)
    public long getWinThreshold() {
        return winThreshold;
    }

    public void setWinThreshold(long winThreshold) {
        this.winThreshold = winThreshold;
    }

    @Column(name = "create_date", nullable = false)
    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    @Column(name = "update_date", nullable = false)
    public Timestamp getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Timestamp updateDate) {
        this.updateDate = updateDate;
    }

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "vote")
    public Set<VotePoint> getVotePoints() {
        return votePoints;
    }

    public void setVotePoints(Set<VotePoint> votePoints) {
        this.votePoints = votePoints;
    }

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "vote")
    public Set<VoteRoleFilter> getRolesFilter() {
        return rolesFilter;
    }

    public void setRolesFilter(Set<VoteRoleFilter> rolesFilter) {
        this.rolesFilter = rolesFilter;
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
                ", rolesFilter=" + rolesFilter +
                '}';
    }

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
}

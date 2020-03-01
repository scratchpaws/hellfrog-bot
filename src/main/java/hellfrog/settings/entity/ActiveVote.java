package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.ActiveVotesDAOImpl;
import hellfrog.settings.db.InstantPersister;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@DatabaseTable(tableName = "active_votes", daoClass = ActiveVotesDAOImpl.class)
public class ActiveVote {

    public static final String ID_FIELD_NAME = "vote_id";
    public static final String SERVER_ID_FIELD_NAME = "server_id";
    public static final String TEXT_CHAT_ID_FIELD_NAME = "text_chat_id";
    public static final String MESSAGE_ID_FIELD_NAME = "message_id";
    public static final String FINISH_TIME_FIELD_NAME = "finish_date";
    public static final String VOTE_TEXT_FIELD_NAME = "vote_text";
    public static final String HAS_TIMER_FIELD_NAME = "has_timer";
    public static final String EXCEPTIONAL_FIELD_NAME = "is_exceptional";
    public static final String HAS_DEFAULT_FIELD_NAME = "has_default";
    public static final String WIN_THRESHOLD_FIELD_NAME = "win_threshold";
    public static final String CREATE_DATE_FIELD_NAME = "create_date";
    public static final String UPDATE_DATE_FIELD_NAME = "update_date";

    @DatabaseField(columnName = ID_FIELD_NAME, canBeNull = false, generatedId = true)
    private long id;
    @DatabaseField(columnName = SERVER_ID_FIELD_NAME, canBeNull = false)
    private long serverId;
    @DatabaseField(columnName = TEXT_CHAT_ID_FIELD_NAME, canBeNull = false)
    private long textChatId;
    @DatabaseField(columnName = MESSAGE_ID_FIELD_NAME, canBeNull = false)
    private long messageId = 0L;
    @DatabaseField(columnName = FINISH_TIME_FIELD_NAME, canBeNull = false,
            defaultValue = "0", persisterClass = InstantPersister.class)
    private Instant finishTime = Instant.ofEpochMilli(0L);
    @DatabaseField(columnName = VOTE_TEXT_FIELD_NAME, canBeNull = false)
    private String voteText;
    @DatabaseField(columnName = HAS_TIMER_FIELD_NAME, canBeNull = false)
    private boolean hasTimer = false;
    @DatabaseField(columnName = EXCEPTIONAL_FIELD_NAME, canBeNull = false)
    private boolean exceptional = false;
    @DatabaseField(columnName = HAS_DEFAULT_FIELD_NAME, canBeNull = false)
    private boolean hasDefault = false;
    @DatabaseField(columnName = WIN_THRESHOLD_FIELD_NAME, canBeNull = false)
    private long winThreshold = 0L;
    @DatabaseField(columnName = CREATE_DATE_FIELD_NAME, canBeNull = false,
            defaultValue = "0", persisterClass = InstantPersister.class)
    private Instant createDate;
    @DatabaseField(columnName = UPDATE_DATE_FIELD_NAME, canBeNull = false,
            defaultValue = "0", persisterClass = InstantPersister.class)
    private Instant updateDate;
    private List<ActiveVotePoint> votePoints = new ArrayList<>();
    private List<VoteRole> rolesFilter = new ArrayList<>();

    public ActiveVote() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public long getTextChatId() {
        return textChatId;
    }

    public void setTextChatId(long textChatId) {
        this.textChatId = textChatId;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public Instant getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Instant finishTime) {
        this.finishTime = finishTime;
    }

    public String getVoteText() {
        return voteText;
    }

    public void setVoteText(String voteText) {
        this.voteText = voteText;
    }

    public boolean isHasTimer() {
        return hasTimer;
    }

    public void setHasTimer(boolean hasTimer) {
        this.hasTimer = hasTimer;
    }

    public boolean isExceptional() {
        return exceptional;
    }

    public void setExceptional(boolean exceptional) {
        this.exceptional = exceptional;
    }

    public boolean isHasDefault() {
        return hasDefault;
    }

    public void setHasDefault(boolean hasDefault) {
        this.hasDefault = hasDefault;
    }

    public long getWinThreshold() {
        return winThreshold;
    }

    public void setWinThreshold(long winThreshold) {
        this.winThreshold = winThreshold;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public Instant getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
    }

    public List<ActiveVotePoint> getVotePoints() {
        return votePoints;
    }

    public void setVotePoints(List<ActiveVotePoint> votePoints) {
        this.votePoints = votePoints;
    }

    public List<VoteRole> getRolesFilter() {
        return rolesFilter;
    }

    public void setRolesFilter(List<VoteRole> rolesFilter) {
        this.rolesFilter = rolesFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveVote that = (ActiveVote) o;
        return id == that.id &&
                serverId == that.serverId &&
                textChatId == that.textChatId &&
                messageId == that.messageId &&
                hasTimer == that.hasTimer &&
                exceptional == that.exceptional &&
                hasDefault == that.hasDefault &&
                winThreshold == that.winThreshold &&
                Objects.equals(finishTime, that.finishTime) &&
                Objects.equals(voteText, that.voteText) &&
                Objects.equals(createDate, that.createDate) &&
                Objects.equals(updateDate, that.updateDate) &&
                Objects.equals(votePoints, that.votePoints) &&
                Objects.equals(rolesFilter, that.rolesFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverId, textChatId, messageId, finishTime, voteText, hasTimer, exceptional, hasDefault, winThreshold, createDate, updateDate, votePoints, rolesFilter);
    }

    @Override
    public String toString() {
        return "ActiveVote{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", textChatId=" + textChatId +
                ", messageId=" + messageId +
                ", finishTime=" + finishTime +
                ", voteText='" + voteText + '\'' +
                ", hasTimer=" + hasTimer +
                ", exceptional=" + exceptional +
                ", hasDefault=" + hasDefault +
                ", winThreshold=" + winThreshold +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                ", votePoints=" + votePoints +
                ", rolesFilter=" + rolesFilter +
                '}';
    }
}

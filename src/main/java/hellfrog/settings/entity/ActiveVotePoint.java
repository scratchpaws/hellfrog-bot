package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.ActiveVotePointsDAOImpl;
import hellfrog.settings.db.InstantPersister;

import java.time.Instant;
import java.util.Objects;

@DatabaseTable(tableName = "vote_points", daoClass = ActiveVotePointsDAOImpl.class)
public class ActiveVotePoint {

    public static final String ID_FIELD_NAME = "id";
    public static final String ACTIVE_VOTE_FIELD_NAME = "vote_id";
    public static final String POINT_TEXT_FIELD_NAME = "point_text";
    public static final String UNICODE_EMOJI_FIELD_NAME = "unicode_emoji";
    public static final String CUSTOM_EMOJI_ID_FIELD_NAME = "custom_emoji_id";
    public static final String CREATE_DATE_FIELD_NAME = "create_date";
    public static final String UPDATE_DATE_FIELD_NAME = "update_date";

    @DatabaseField(columnName = ID_FIELD_NAME, generatedId = true, canBeNull = false)
    private long id;
    @DatabaseField(columnName = ACTIVE_VOTE_FIELD_NAME, canBeNull = false, foreign = true)
    private ActiveVote activeVote;
    @DatabaseField(columnName = POINT_TEXT_FIELD_NAME, canBeNull = false)
    private String pointText;
    @DatabaseField(columnName = UNICODE_EMOJI_FIELD_NAME)
    private String unicodeEmoji = null;
    @DatabaseField(columnName = CUSTOM_EMOJI_ID_FIELD_NAME)
    private long customEmojiId = 0L;
    @DatabaseField(columnName = CREATE_DATE_FIELD_NAME, canBeNull = false,
            defaultValue = "0", persisterClass = InstantPersister.class)
    private Instant createDate;
    @DatabaseField(columnName = UPDATE_DATE_FIELD_NAME, canBeNull = false,
            defaultValue = "0", persisterClass = InstantPersister.class)
    private Instant updateDate;

    public ActiveVotePoint() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ActiveVote getActiveVote() {
        return activeVote;
    }

    public void setActiveVote(ActiveVote activeVote) {
        this.activeVote = activeVote;
    }

    public String getPointText() {
        return pointText;
    }

    public void setPointText(String pointText) {
        this.pointText = pointText;
    }

    public String getUnicodeEmoji() {
        return unicodeEmoji;
    }

    public void setUnicodeEmoji(String unicodeEmoji) {
        this.unicodeEmoji = unicodeEmoji;
    }

    public long getCustomEmojiId() {
        return customEmojiId;
    }

    public void setCustomEmojiId(long customEmojiId) {
        this.customEmojiId = customEmojiId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveVotePoint that = (ActiveVotePoint) o;
        long activeVoteId = activeVote != null ? activeVote.getId() : -1L;
        long thatActiveVoteId = that.activeVote != null ? that.activeVote.getId() : -1L;
        return id == that.id &&
                customEmojiId == that.customEmojiId &&
                activeVoteId == thatActiveVoteId &&
                Objects.equals(pointText, that.pointText) &&
                Objects.equals(unicodeEmoji, that.unicodeEmoji) &&
                Objects.equals(createDate, that.createDate) &&
                Objects.equals(updateDate, that.updateDate);
    }

    @Override
    public int hashCode() {
        long activeVoteId = activeVote != null ? activeVote.getId() : -1L;
        return Objects.hash(id, activeVoteId, pointText, unicodeEmoji, customEmojiId, createDate, updateDate);
    }

    @Override
    public String toString() {
        long activeVoteId = activeVote != null ? activeVote.getId() : -1L;
        return "ActiveVotePoint{" +
                "id=" + id +
                ", activeVoteId=" + activeVoteId +
                ", pointText='" + pointText + '\'' +
                ", unicodeEmoji='" + unicodeEmoji + '\'' +
                ", customEmojiId=" + customEmojiId +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}

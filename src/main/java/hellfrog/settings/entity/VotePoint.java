package hellfrog.settings.entity;

import hellfrog.common.CommonUtils;

import java.time.Instant;
import java.util.Optional;

public class VotePoint {

    public enum Columns {

        ID(14, -1),
        VOTE_ID(15, 1),
        POINT_TEXT(16, 2),
        UNICODE_EMOJI(17, 3),
        CUSTOM_EMOJI_ID(18, 4),
        CREATE_DATE(19, 5),
        UPDATE_DATE(20, 6);

        public final int selectColumn;
        public final int insertColumn;

        Columns(int selectColumn, int insertColumn) {
            this.selectColumn = selectColumn;
            this.insertColumn = insertColumn;
        }
    }

    private long id = -1L;
    private String pointText = null;
    private String unicodeEmoji = null;
    private long customEmojiId = 0L;
    private Instant createDate = null;
    private Instant updateDate = null;

    public VotePoint() {}

    public long getId() {
        return id;
    }

    public VotePoint setId(long id) {
        this.id = id;
        return this;
    }

    public Optional<String> getPointText() {
        return Optional.ofNullable(pointText);
    }

    public VotePoint setPointText(String pointText) {
        this.pointText = CommonUtils.isTrStringNotEmpty(pointText) ? pointText : null;
        return this;
    }

    public Optional<String> getUnicodeEmoji() {
        return Optional.ofNullable(unicodeEmoji);
    }

    public VotePoint setUnicodeEmoji(String unicodeEmoji) {
        this.unicodeEmoji = CommonUtils.isTrStringNotEmpty(pointText) ? unicodeEmoji : null;
        return this;
    }

    public long getCustomEmojiId() {
        return customEmojiId;
    }

    public VotePoint setCustomEmojiId(long customEmojiId) {
        this.customEmojiId = customEmojiId;
        return this;
    }

    public Optional<Instant> getCreateDate() {
        return Optional.ofNullable(createDate);
    }

    public VotePoint setCreateDate(Instant createDate) {
        this.createDate = createDate;
        return this;
    }

    public Optional<Instant> getUpdateDate() {
        return Optional.ofNullable(updateDate);
    }

    public VotePoint setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    @Override
    public String toString() {
        return "VotePoint{" +
                "id=" + id +
                ", pointText='" + pointText + '\'' +
                ", unicodeEmoji='" + unicodeEmoji + '\'' +
                ", customEmojiId=" + customEmojiId +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}

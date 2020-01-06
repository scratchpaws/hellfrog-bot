package hellfrog.settings.entity;

import java.time.Instant;

public class VotePoint {

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

    public String getPointText() {
        return pointText;
    }

    public VotePoint setPointText(String pointText) {
        this.pointText = pointText;
        return this;
    }

    public String getUnicodeEmoji() {
        return unicodeEmoji;
    }

    public VotePoint setUnicodeEmoji(String unicodeEmoji) {
        this.unicodeEmoji = unicodeEmoji;
        return this;
    }

    public long getCustomEmojiId() {
        return customEmojiId;
    }

    public VotePoint setCustomEmojiId(long customEmojiId) {
        this.customEmojiId = customEmojiId;
        return this;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public VotePoint setCreateDate(Instant createDate) {
        this.createDate = createDate;
        return this;
    }

    public Instant getUpdateDate() {
        return updateDate;
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

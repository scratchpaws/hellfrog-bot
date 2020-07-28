package hellfrog.settings.oldjson;

import java.util.Objects;

// hellfrog.settings.db.entity.VotePoint
public class JSONVotePoint {

    // hellfrog.settings.db.entity.VotePoint.id
    private long id;
    // hellfrog.settings.db.entity.VotePoint.unicodeEmoji
    private String emoji;
    // hellfrog.settings.db.entity.VotePoint.customEmojiId
    private Long customEmoji;
    // hellfrog.settings.db.entity.VotePoint.pointText
    private String pointText;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Long getCustomEmoji() {
        return customEmoji;
    }

    public void setCustomEmoji(Long customEmoji) {
        this.customEmoji = customEmoji;
    }

    public String getPointText() {
        return pointText;
    }

    public void setPointText(String pointText) {
        this.pointText = pointText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JSONVotePoint votePoint = (JSONVotePoint) o;
        return id == votePoint.id &&
                Objects.equals(emoji, votePoint.emoji) &&
                Objects.equals(customEmoji, votePoint.customEmoji) &&
                Objects.equals(pointText, votePoint.pointText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, emoji, customEmoji, pointText);
    }

    @Override
    public String toString() {
        return "VotePoint{" +
                "id=" + id +
                ", emoji='" + emoji + '\'' +
                ", CustomEmoji=" + customEmoji +
                ", pointText='" + pointText + '\'' +
                '}';
    }
}

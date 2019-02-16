package pub.funforge.scratchypaws.rilcobot.settings.old;

import java.util.Objects;

/**
 * Пункт в голосовании
 */
public class VotePoint {

    private long id;
    private String emoji;
    private Long CustomEmoji;
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
        return CustomEmoji;
    }

    public void setCustomEmoji(Long customEmoji) {
        CustomEmoji = customEmoji;
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
        VotePoint votePoint = (VotePoint) o;
        return id == votePoint.id &&
                Objects.equals(emoji, votePoint.emoji) &&
                Objects.equals(CustomEmoji, votePoint.CustomEmoji) &&
                Objects.equals(pointText, votePoint.pointText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, emoji, CustomEmoji, pointText);
    }

    @Override
    public String toString() {
        return "VotePoint{" +
                "id=" + id +
                ", emoji='" + emoji + '\'' +
                ", CustomEmoji=" + CustomEmoji +
                ", pointText='" + pointText + '\'' +
                '}';
    }
}

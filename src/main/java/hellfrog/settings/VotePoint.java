package hellfrog.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hellfrog.common.CommonUtils;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;

import java.util.Map;
import java.util.Objects;

/**
 * Пункт в голосовании
 */
@Deprecated
public class VotePoint {

    private long id;
    private String emoji;
    private Long customEmoji;
    private String pointText;

    @Deprecated
    public long getId() {
        return id;
    }

    @Deprecated
    public void setId(long id) {
        this.id = id;
    }

    @Deprecated
    public String getEmoji() {
        return emoji;
    }

    @Deprecated
    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    @Deprecated
    public Long getCustomEmoji() {
        return customEmoji;
    }

    @Deprecated
    public void setCustomEmoji(Long customEmoji) {
        this.customEmoji = customEmoji;
    }

    @Deprecated
    public String getPointText() {
        return pointText;
    }

    @Deprecated
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

    @Deprecated
    @JsonIgnore
    public String buildVoteString(Map<Long, KnownCustomEmoji> emojiCache) {
        StringBuilder voteString = new StringBuilder();
        if (this.isUnicodeVP()) {
            voteString.append(emoji);
        } else if (this.isCustomEmojiVP() && emojiCache != null && emojiCache.containsKey(customEmoji)) {
            voteString.append(emojiCache.get(customEmoji).getMentionTag());
        } else {
            voteString.append("[unknown emoji]");
        }
        voteString.append(" - ")
                .append(this.getPointText());
        return voteString.toString();
    }

    @Deprecated
    public boolean equalsEmoji(Emoji external) {
        if (external == null) return false;
        if (external.isUnicodeEmoji() && external.asUnicodeEmoji().isPresent()) {
            String unicodeEmoji = external.asUnicodeEmoji().get();
            return !CommonUtils.isTrStringEmpty(emoji)
                    && unicodeEmoji.equals(emoji);
        } else if (external.isCustomEmoji() && external.asCustomEmoji().isPresent()) {
            long customId = external.asCustomEmoji().get().getId();
            return customEmoji != null
                    && customEmoji > 0
                    && customId == customEmoji;
        }
        return false;
    }

    @Deprecated
    @JsonIgnore
    public boolean isUnicodeVP() {
        return !CommonUtils.isTrStringEmpty(emoji);
    }

    @Deprecated
    @JsonIgnore
    public boolean isCustomEmojiVP() {
        return customEmoji != null && customEmoji > 0;
    }
}

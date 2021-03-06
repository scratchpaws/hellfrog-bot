package hellfrog.settings.db.entity;

import hellfrog.common.CommonUtils;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "vote_points")
public class VotePoint {

    private long id;
    private Vote vote;
    private String pointText;
    private String unicodeEmoji;
    private long customEmojiId;
    private Timestamp createDate;
    private Timestamp updateDate;
    public VotePoint() {}

    @PrePersist
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
        if (updateDate == null) {
            updateDate = Timestamp.from(Instant.now());
        }
        if (CommonUtils.isTrStringEmpty(pointText)) {
            pointText = "";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateDate = Timestamp.from(Instant.now());
    }

    @Id
    @GeneratedValue(generator = "vote_point_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "vote_point_ids", sequenceName = "vote_point_ids")
    @Column(name = "id", nullable = false, unique = true)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vote_id", nullable = false)
    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }

    @Column(name = "point_text", nullable = false, length = 2000)
    public String getPointText() {
        return pointText;
    }

    public void setPointText(String pointText) {
        this.pointText = pointText;
    }

    @Column(name = "unicode_emoji", length = 12)
    public String getUnicodeEmoji() {
        return unicodeEmoji;
    }

    public void setUnicodeEmoji(String unicodeEmoji) {
        this.unicodeEmoji = unicodeEmoji;
    }

    @Column(name = "custom_emoji_id")
    public long getCustomEmojiId() {
        return customEmojiId;
    }

    public void setCustomEmojiId(long customEmojiId) {
        this.customEmojiId = customEmojiId;
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

    @Override
    public String toString() {
        return "VotePoint{" +
                "id=" + id +
                ", vote id=" + (vote != null ? vote.getId() : "(null)") +
                ", pointText='" + pointText + '\'' +
                ", unicodeEmoji='" + unicodeEmoji + '\'' +
                ", customEmojiId=" + customEmojiId +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}

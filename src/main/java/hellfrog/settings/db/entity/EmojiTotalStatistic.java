package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "emoji_total_statistics", indexes = {
        @Index(name = "uniq_total_emoji_stat", columnList = "server_id,emoji_id", unique = true)
})
public class EmojiTotalStatistic {

    private long id;
    private long serverId;
    private long emojiId;
    private long usagesCount;
    private Timestamp lastUsage;
    private Timestamp createDate;
    private Timestamp updateDate;

    @PrePersist
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
        if (updateDate == null) {
            updateDate = Timestamp.from(Instant.now());
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateDate = Timestamp.from(Instant.now());
    }

    @Id
    @GeneratedValue(generator = "emoji_total_stat_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "emoji_total_stat_ids", sequenceName = "emoji_total_stat_ids")
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

    @Column(name = "emoji_id", nullable = false)
    public long getEmojiId() {
        return emojiId;
    }

    public void setEmojiId(long emojiId) {
        this.emojiId = emojiId;
    }

    @Column(name = "usages_count", nullable = false)
    public long getUsagesCount() {
        return usagesCount;
    }

    public void setUsagesCount(long usagesCount) {
        this.usagesCount = usagesCount;
    }

    @Column(name = "last_usage", nullable = false)
    public Timestamp getLastUsage() {
        return lastUsage;
    }

    public void setLastUsage(Timestamp lastUsage) {
        this.lastUsage = lastUsage;
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
        return "EmojiTotalStatistic{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", emojiId=" + emojiId +
                ", usagesCount=" + usagesCount +
                ", lastUsage=" + lastUsage +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmojiTotalStatistic that = (EmojiTotalStatistic) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

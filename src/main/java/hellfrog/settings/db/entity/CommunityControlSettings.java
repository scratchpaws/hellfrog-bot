package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "community_control_settings")
public class CommunityControlSettings {

    private long id;
    private long serverId;
    private long roleId;
    private long threshold;
    private String unicodeEmoji;
    private long customEmojiId;
    private Timestamp createDate;
    private Timestamp updateDate;

    @Id
    @GeneratedValue(generator = "community_control_setting_idx", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "community_control_setting_idx", sequenceName = "community_control_setting_idx")
    @Column(name = "id", nullable = false, unique = true)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name = "server_id", nullable = false, unique = true)
    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    @Column(name = "assign_role_id", nullable = false)
    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    @Column(name = "threshold", nullable = false)
    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    @Column(name = "unicode_emoji", length = 12)
    public String getUnicodeEmoji() {
        return unicodeEmoji;
    }

    public void setUnicodeEmoji(String unicodeEmoji) {
        this.unicodeEmoji = unicodeEmoji;
    }

    @Column(name = "custom_emoji_id", nullable = false)
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

    @PrePersist
    void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
        if (updateDate == null) {
            updateDate = Timestamp.from(Instant.now());
        }
    }

    @PreUpdate
    void preUpdate() {
        updateDate = Timestamp.from(Instant.now());
    }

    @Override
    public String toString() {
        return "CommunityControlSettings{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", roleId=" + roleId +
                ", threshold=" + threshold +
                ", unicodeEmoji='" + unicodeEmoji + '\'' +
                ", customEmojiId=" + customEmojiId +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}

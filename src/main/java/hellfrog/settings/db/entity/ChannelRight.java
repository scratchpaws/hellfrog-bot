package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "channel_rights", indexes = {
        @Index(name = "uniq_channel_right", columnList = "server_id,command_prefix,channel_id", unique = true),
        @Index(name = "channel_right_idx", columnList = "server_id,command_prefix")
})
public class ChannelRight implements EntityRight {

    private long id;
    private long serverId;
    private String commandPrefix;
    private long channelId;
    private Timestamp createDate;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
    }

    @Override
    @Transient
    public long getEntityId() {
        return channelId;
    }

    @Id
    @GeneratedValue(generator = "channel_right_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "channel_right_ids", sequenceName = "channel_right_ids")
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

    @Column(name = "command_prefix", nullable = false, length = 20)
    public String getCommandPrefix() {
        return commandPrefix;
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    @Column(name = "channel_id", nullable = false)
    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    @Column(name = "create_date", nullable = false)
    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelRight that = (ChannelRight) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TextChannelRight{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", commandPrefix='" + commandPrefix + '\'' +
                ", channelId=" + channelId +
                ", createDate=" + createDate +
                '}';
    }
}

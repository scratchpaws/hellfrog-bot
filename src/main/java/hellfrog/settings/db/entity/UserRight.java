package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_rights", indexes = {
        @Index(name = "uniq_user_right", columnList = "server_id,command_prefix,user_id", unique = true)
})
public class UserRight implements EntityRight {

    private long id;
    private long serverId;
    private String commandPrefix;
    private long userId;
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
        return userId;
    }

    @Id
    @GeneratedValue(generator = "user_right_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "user_right_ids", sequenceName = "user_right_ids")
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

    @Column(name = "user_id", nullable = false)
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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
        UserRight userRight = (UserRight) o;
        return id == userRight.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserRight{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", commandPrefix='" + commandPrefix + '\'' +
                ", userId=" + userId +
                ", createDate=" + createDate +
                '}';
    }
}

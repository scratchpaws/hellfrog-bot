package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "community_control_users", indexes = {
        @Index(name = "uniq_community_control_user", unique = true, columnList = "server_id,user_id"),
        @Index(name = "community_control_users_srv_idx", columnList = "server_id")
})
public class CommunityControlUser {

    private long id;
    private long serverId;
    private long userId;
    private Timestamp createDate;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
    }

    @Id
    @GeneratedValue(generator = "community_control_user_idx", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "community_control_user_idx", sequenceName = "community_control_user_idx")
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
    public String toString() {
        return "CommunityControlUser{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", userId=" + userId +
                ", createDate=" + createDate +
                '}';
    }
}

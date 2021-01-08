package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "role_assign_queue", indexes = {
        @Index(name = "role_assign_queue_servers", columnList = "server_id")
})
public class RoleAssign {

    private long id;
    private long serverId;
    private long userId;
    private long roleId;
    private Timestamp assignDate;
    private Timestamp createDate;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
    }

    @Id
    @GeneratedValue(generator = "role_assign_queue_idx", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "role_assign_queue_idx", sequenceName = "role_assign_queue_idx")
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

    @Column(name = "role_id", nullable = false)
    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    @Column(name = "assign_date", nullable = false)
    public Timestamp getAssignDate() {
        return assignDate;
    }

    public void setAssignDate(Timestamp assignDate) {
        this.assignDate = assignDate;
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
        return "RoleAssign{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", userId=" + userId +
                ", roleId=" + roleId +
                ", assignDate=" + assignDate +
                ", createDate=" + createDate +
                '}';
    }
}

package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "auto_promote_configs", indexes = {
        @Index(name = "uniq_auto_promote_role_cfg", unique = true, columnList = "server_id,role_id")
})
public class AutoPromoteConfig {

    private long id;
    private long serverId;
    private long roleId;
    private long timeout;
    private Timestamp createDate;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
    }

    @Id
    @GeneratedValue(generator = "auto_promote_config_idx", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "auto_promote_config_idx", sequenceName = "auto_promote_config_idx")
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

    @Column(name = "role_id", nullable = false)
    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    @Column(name = "timeout", nullable = false)
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
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
        return "AutoPromoteConfig{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", roleId=" + roleId +
                ", timeout=" + timeout +
                ", createDate=" + createDate +
                '}';
    }
}

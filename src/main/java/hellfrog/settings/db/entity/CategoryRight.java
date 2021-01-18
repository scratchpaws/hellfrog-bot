package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "category_rights", indexes = {
        @Index(name = "uniq_category_right", columnList = "server_id,command_prefix,category_id", unique = true),
        @Index(name = "category_right_idx", columnList = "server_id,command_prefix")
})
public class CategoryRight implements EntityRight {

    private long id;
    private long serverId;
    private String commandPrefix;
    private long categoryId;
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
        return categoryId;
    }

    @Id
    @GeneratedValue(generator = "category_right_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "category_right_ids", sequenceName = "category_right_ids")
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

    @Column(name = "category_id", nullable = false)
    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
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
        CategoryRight that = (CategoryRight) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CategoryRight{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", commandPrefix='" + commandPrefix + '\'' +
                ", categoryId=" + categoryId +
                ", createDate=" + createDate +
                '}';
    }
}

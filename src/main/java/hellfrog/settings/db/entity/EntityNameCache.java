package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "names_cache")
public class EntityNameCache {

    private long entityId;
    private String name;
    private NameType nameType;
    private Timestamp createDate;
    private Timestamp updateDate;

    @Id
    @Column(name = "entity_id", nullable = false, unique = true)
    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    @Column(name = "entity_name", nullable = false, length = 120)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "entity_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    public NameType getEntityType() {
        return nameType;
    }

    public void setEntityType(NameType nameType) {
        this.nameType = nameType;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityNameCache nameCache = (EntityNameCache) o;
        return entityId == nameCache.entityId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId);
    }
}

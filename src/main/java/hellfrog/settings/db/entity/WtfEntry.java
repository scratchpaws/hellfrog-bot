package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;

/**
 * Descriptions of one participant that another participant gave him
 */
@Entity
@Table(name = "wtf_assigns", indexes = {
        @Index(name = "uniq_wft_assign", unique = true, columnList = "server_id,author_id,target_id")
})
public class WtfEntry {

    private long id;
    private long serverId;
    private long authorId;
    private long targetId;
    private String description;
    private Set<WtfEntryAttach> attaches;
    private Timestamp createDate;
    private Timestamp updateDate;

    public WtfEntry() {
    }

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
    @GeneratedValue(generator = "wtf_assign_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "wtf_assign_ids", sequenceName = "wtf_assign_ids")
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

    @Column(name = "author_id", nullable = false)
    public long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(long authorId) {
        this.authorId = authorId;
    }

    @Column(name = "target_id", nullable = false)
    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    @Column(name = "description", length = 2000)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "wtfEntry")
    public Set<WtfEntryAttach> getAttaches() {
        return attaches;
    }

    public void setAttaches(Set<WtfEntryAttach> attaches) {
        this.attaches = attaches;
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
        return "WtfEntry{" +
                "\n  id=" + id +
                ",\n  serverId=" + serverId +
                ",\n  authorId=" + authorId +
                ",\n  targetId=" + targetId +
                ",\n  description='" + description + '\'' +
                ",\n  attaches=" + attaches +
                ",\n  createDate=" + createDate +
                ",\n  updateDate=" + updateDate +
                "\n}";
    }
}

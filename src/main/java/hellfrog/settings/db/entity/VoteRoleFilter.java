package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "vote_roles", indexes = {
        @Index(name = "vote_roles_msg", columnList = "message_id"),
        @Index(name = "uniq_vote_role", columnList = "vote_id,role_id", unique = true)
})
public class VoteRoleFilter {

    private long id;
    private Vote vote;
    private long messageId;
    private long roleId;
    private Timestamp createDate;
    private Timestamp updateDate;

    public VoteRoleFilter() {
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
    @GeneratedValue(generator = "vote_role_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "vote_role_ids", sequenceName = "vote_role_ids")
    @Column(name = "id", nullable = false, unique = true)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vote_id", nullable = false)
    public Vote getVote() {
        return vote;
    }

    public void setVote(Vote vote) {
        this.vote = vote;
    }

    @Column(name = "message_id", nullable = false)
    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    @Column(name = "role_id", nullable = false)
    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
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
        VoteRoleFilter that = (VoteRoleFilter) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "VoteRoleFilter{" +
                "id=" + id +
                ", vote id=" + (vote != null ? vote.getId() : "(null)") +
                ", messageId=" + messageId +
                ", roleId=" + roleId +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}

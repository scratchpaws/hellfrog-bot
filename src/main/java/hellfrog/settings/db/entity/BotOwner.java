package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "bot_owners")
public class BotOwner {

    private long userId;
    private Timestamp createDate;

    public BotOwner() {
    }

    public BotOwner(long userId, Timestamp createDate) {
        this.userId = userId;
        this.createDate = createDate;
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
    }

    @Id
    @Column(name = "user_id", nullable = false, unique = true)
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
        BotOwner botOwner = (BotOwner) o;
        return userId == botOwner.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "BotOwner{" +
                "userId=" + userId +
                ", createDate=" + createDate +
                '}';
    }
}

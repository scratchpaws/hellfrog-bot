package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;

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
    public boolean equals(Object obj) {
        return obj instanceof BotOwner && this.userId == ((BotOwner)obj).userId;
    }
}

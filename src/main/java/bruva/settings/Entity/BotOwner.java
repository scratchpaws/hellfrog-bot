package bruva.settings.Entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "bot_owners")
public class BotOwner {

    private long userId;

    @Id
    @Column(name = "user_id", nullable = false)
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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
}

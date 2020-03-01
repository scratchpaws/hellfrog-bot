package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.BotOwnersDAOImpl;
import hellfrog.settings.db.InstantPersister;

import java.time.Instant;
import java.util.Objects;

@DatabaseTable(tableName = "bot_owners", daoClass = BotOwnersDAOImpl.class)
public class BotOwner {

    public static final String USER_ID_FIELD_NAME = "user_id";
    public static final String CREATE_DATE_FIELD_NAME = "create_date";

    @DatabaseField(columnName = USER_ID_FIELD_NAME, id = true, unique = true, canBeNull = false)
    private long userId;

    @DatabaseField(columnName = CREATE_DATE_FIELD_NAME, canBeNull = false, persisterClass = InstantPersister.class)
    private Instant createDate;

    public BotOwner() {
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "BotOwner{" +
                "userId=" + userId +
                ", createDate=" + createDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotOwner botOwner = (BotOwner) o;
        return userId == botOwner.userId &&
                Objects.equals(createDate, botOwner.createDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, createDate);
    }
}

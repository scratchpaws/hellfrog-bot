package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.InstantPersister;
import hellfrog.settings.db.UserRightsDAOImpl;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

@DatabaseTable(tableName = "user_rights", daoClass = UserRightsDAOImpl.class)
public class UserRight
        implements RightEntity {

    public static final String USER_ID_FIELD_NAME = "user_id";

    @DatabaseField(generatedId = true, unique = true, columnName = ID_FIELD_NAME, canBeNull = false)
    private long id;
    @DatabaseField(columnName = SERVER_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_user_right")
    private long serverId;
    @DatabaseField(columnName = COMMAND_PREFIX_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_user_right")
    private String commandPrefix;
    @DatabaseField(columnName = USER_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_user_right")
    private long userId;
    @DatabaseField(columnName = CREATE_DATE_FIELD_NAME, canBeNull = false, persisterClass = InstantPersister.class)
    private Instant createDate;

    public UserRight() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public long getServerId() {
        return serverId;
    }

    @Override
    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    @Override
    public String getCommandPrefix() {
        return commandPrefix;
    }

    @Override
    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Override
    public long getEntityId() {
        return userId;
    }

    @Override
    public void setEntityId(long newEntityId) {
        this.userId = newEntityId;
    }

    @Override
    public Instant getCreateDate() {
        return createDate;
    }

    @Override
    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRight userRight = (UserRight) o;
        return id == userRight.id &&
                serverId == userRight.serverId &&
                userId == userRight.userId &&
                Objects.equals(commandPrefix, userRight.commandPrefix) &&
                Objects.equals(createDate, userRight.createDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverId, commandPrefix, userId, createDate);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UserRight.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("serverId=" + serverId)
                .add("commandPrefix='" + commandPrefix + "'")
                .add("userId=" + userId)
                .add("createDate=" + createDate)
                .toString();
    }
}

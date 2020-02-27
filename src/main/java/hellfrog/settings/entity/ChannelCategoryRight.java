package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.InstantPersister;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

@DatabaseTable(tableName = "category_rights")
public class ChannelCategoryRight
        implements RightEntity {

    public static final String CATEGORY_ID_FIELD_NAME = "category_id";

    @DatabaseField(generatedId = true, unique = true, columnName = ID_FIELD_NAME, canBeNull = false)
    private long id;
    @DatabaseField(columnName = SERVER_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_category_right")
    private long serverId;
    @DatabaseField(columnName = COMMAND_PREFIX_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_category_right")
    private String commandPrefix;
    @DatabaseField(columnName = CATEGORY_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_category_right")
    private long categoryId;
    @DatabaseField(columnName = CREATE_DATE_FIELD_NAME, canBeNull = false, persisterClass = InstantPersister.class)
    private Instant createDate;

    public ChannelCategoryRight() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public void setCommandPrefix(String commandPrefix) {
        this.commandPrefix = commandPrefix;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelCategoryRight that = (ChannelCategoryRight) o;
        return id == that.id &&
                serverId == that.serverId &&
                categoryId == that.categoryId &&
                Objects.equals(commandPrefix, that.commandPrefix) &&
                Objects.equals(createDate, that.createDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverId, commandPrefix, categoryId, createDate);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ChannelCategoryRight.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("serverId=" + serverId)
                .add("commandPrefix='" + commandPrefix + "'")
                .add("categoryId=" + categoryId)
                .add("createDate=" + createDate)
                .toString();
    }
}

package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.InstantPersister;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

@DatabaseTable(tableName = "text_channel_rights")
public class TextChannelRight
        implements RightEntity {

    public static final String CHANNEL_ID_FIELD_NAME = "channel_id";

    @DatabaseField(generatedId = true, unique = true, columnName = ID_FIELD_NAME, canBeNull = false)
    private long id;
    @DatabaseField(columnName = SERVER_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_channel_right")
    private long serverId;
    @DatabaseField(columnName = COMMAND_PREFIX_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_channel_right")
    private String commandPrefix;
    @DatabaseField(columnName = CHANNEL_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_channel_right")
    private long channelId;
    @DatabaseField(columnName = CREATE_DATE_FIELD_NAME, canBeNull = false, persisterClass = InstantPersister.class)
    private Instant createDate;

    public TextChannelRight() {
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

    public long getChannelId() {
        return channelId;
    }

    public void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    @Override
    public long getEntityId() {
        return channelId;
    }

    @Override
    public void setEntityId(long newEntityId) {
        this.channelId = newEntityId;
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
        TextChannelRight that = (TextChannelRight) o;
        return id == that.id &&
                serverId == that.serverId &&
                channelId == that.channelId &&
                Objects.equals(commandPrefix, that.commandPrefix) &&
                Objects.equals(createDate, that.createDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverId, commandPrefix, channelId, createDate);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TextChannelRight.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("serverId=" + serverId)
                .add("commandPrefix='" + commandPrefix + "'")
                .add("channelId=" + channelId)
                .add("createDate=" + createDate)
                .toString();
    }
}

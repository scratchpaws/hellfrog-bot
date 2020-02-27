package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.InstantPersister;
import hellfrog.settings.db.ServerPreferencesDAOImpl;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

@DatabaseTable(tableName = "server_preferences", daoClass = ServerPreferencesDAOImpl.class)
public class ServerPreference {

    public static final String ID_FIELD_NAME = "id";
    public static final String SERVER_ID_FIELD_NAME = "server_id";
    public static final String KEY_FIELD_NAME = "key";
    public static final String STRING_VALUE_FIELD_NAME = "string_value";
    public static final String BOOLEAN_VALUE_FIELD_NAME = "boolean_value";
    public static final String NUMERIC_VALUE_FIELD_NAME = "numeric_value";

    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private long id;
    @DatabaseField(columnName = SERVER_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_serv_key")
    private long serverId;
    @DatabaseField(columnName = KEY_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_serv_key")
    private String key;
    @DatabaseField(columnName = STRING_VALUE_FIELD_NAME)
    private String stringValue;
    @DatabaseField(columnName = NUMERIC_VALUE_FIELD_NAME)
    private long longValue;
    @DatabaseField(columnName = BOOLEAN_VALUE_FIELD_NAME)
    private boolean booleanValue;
    @DatabaseField(columnName = "create_date", canBeNull = false, persisterClass = InstantPersister.class)
    private Instant createDate;
    @DatabaseField(columnName = "update_date", canBeNull = false, persisterClass = InstantPersister.class)
    private Instant updateDate;

    public ServerPreference() {
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public boolean isBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }

    public Instant getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Instant updateDate) {
        this.updateDate = updateDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerPreference that = (ServerPreference) o;
        return id == that.id &&
                serverId == that.serverId &&
                longValue == that.longValue &&
                booleanValue == that.booleanValue &&
                Objects.equals(key, that.key) &&
                Objects.equals(stringValue, that.stringValue) &&
                Objects.equals(createDate, that.createDate) &&
                Objects.equals(updateDate, that.updateDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverId, key, stringValue, longValue, booleanValue, createDate, updateDate);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ServerPreference.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("serverId=" + serverId)
                .add("key='" + key + "'")
                .add("stringValue='" + stringValue + "'")
                .add("longValue=" + longValue)
                .add("booleanValue=" + booleanValue)
                .add("createDate=" + createDate)
                .add("updateDate=" + updateDate)
                .toString();
    }
}

package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.CommonPreferencesDAOImpl;
import hellfrog.settings.db.InstantPersister;

import java.time.Instant;
import java.util.Objects;

@DatabaseTable(tableName = "common_preferences", daoClass = CommonPreferencesDAOImpl.class)
public class CommonPreference {

    public static final String KEY_FIELD_NAME = "key";
    public static final String STRING_VALUE_FIELD_NAME = "string_value";
    public static final String NUMERIC_VALUE_FIELD_NAME = "numeric_value";

    @DatabaseField(columnName = KEY_FIELD_NAME, id = true, unique = true, canBeNull = false)
    private String key;
    @DatabaseField(columnName = STRING_VALUE_FIELD_NAME)
    private String stringValue;
    @DatabaseField(columnName = NUMERIC_VALUE_FIELD_NAME)
    private long longValue;
    @DatabaseField(columnName = "create_date", canBeNull = false, persisterClass = InstantPersister.class)
    private Instant createDate;
    @DatabaseField(columnName = "update_date", canBeNull = false, persisterClass = InstantPersister.class)
    private Instant updateDate;

    public CommonPreference() {}

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

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommonPreference that = (CommonPreference) o;
        return longValue == that.longValue &&
                Objects.equals(key, that.key) &&
                Objects.equals(stringValue, that.stringValue) &&
                Objects.equals(createDate, that.createDate) &&
                Objects.equals(updateDate, that.updateDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, stringValue, longValue, createDate, updateDate);
    }

    @Override
    public String toString() {
        return "CommonPreference{" +
                "key='" + key + '\'' +
                ", stringValue='" + stringValue + '\'' +
                ", longValue=" + longValue +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}

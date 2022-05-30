package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "common_preferences")
public class CommonPreference {

    private String key;
    private String stringValue;
    private long longValue;
    private Timestamp createDate;
    private Timestamp updateDate;

    public CommonPreference() {
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
    @Column(name = "`KEY`", unique = true, nullable = false, length = 60)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Column(name = "string_value", nullable = false, length = 64)
    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    @Column(name = "long_value", nullable = false)
    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
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
        CommonPreference that = (CommonPreference) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
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

package hellfrog.settings.db.entity;

import hellfrog.settings.db.h2.BooleanToLongConverter;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "server_preferences", uniqueConstraints =
@UniqueConstraint(name = "uniq_serv_key", columnNames = {"server_id", "`KEY`"}))
public class ServerPreference {

    private long id;
    private long serverId;
    private ServerPrefKey key;
    private String stringValue;
    private long longValue;
    private boolean boolValue;
    private Timestamp dateValue;
    private Timestamp createDate;
    private Timestamp updateDate;

    public ServerPreference() {
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
    @GeneratedValue(generator = "server_preference_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "server_preference_ids", sequenceName = "server_preference_ids")
    @Column(name = "id", nullable = false, unique = true)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name = "server_id", nullable = false)
    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    @Column(name = "`KEY`", nullable = false, length = 60)
    @Enumerated(EnumType.STRING)
    public ServerPrefKey getKey() {
        return key;
    }

    public void setKey(ServerPrefKey key) {
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

    @Column(name = "bool_value", nullable = false)
    @Convert(converter = BooleanToLongConverter.class)
    public boolean isBoolValue() {
        return boolValue;
    }

    public void setBoolValue(boolean boolValue) {
        this.boolValue = boolValue;
    }

    @Column(name = "date_value", nullable = false)
    public Timestamp getDateValue() {
        return dateValue;
    }

    public void setDateValue(Timestamp dateValue) {
        this.dateValue = dateValue;
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
    public String toString() {
        return "ServerPreference{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", key=" + key +
                ", stringValue='" + stringValue + '\'' +
                ", longValue=" + longValue +
                ", boolValue=" + boolValue +
                ", dateValue=" + dateValue +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}

package bruva.settings.Entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "common_preferences",
    uniqueConstraints = {
        @UniqueConstraint(name = "common_preferences_uniq", columnNames = "name")
    })
public class CommonSetting
    implements Serializable {

    private long id;
    private String name;
    private String value;
    private Date insertDate;
    private Date updateDate;
    private String comment;

    @Id
    @GeneratedValue(generator = "common_settings_ids")
    @SequenceGenerator(name = "common_settings_ids", sequenceName = "common_settings_ids")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name = "name", length = 60, nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "value", nullable = false)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "insert_date", nullable = false)
    public Date getInsertDate() {
        return insertDate;
    }

    public void setInsertDate(Date insertDate) {
        this.insertDate = insertDate;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_date", nullable = false)
    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    @Column(name = "\"COMMENT\"", nullable = false)
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommonSetting that = (CommonSetting) o;
        return id == that.id &&
                Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(insertDate, that.insertDate) &&
                Objects.equals(updateDate, that.updateDate) &&
                Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, value, insertDate, updateDate, comment);
    }

    @Override
    public String toString() {
        return "CommonSetting{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", insertDate=" + insertDate +
                ", updateDate=" + updateDate +
                ", comment='" + comment + '\'' +
                '}';
    }
}

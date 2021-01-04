package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "wtf_assigns_attaches", indexes = {
        @Index(name = "uniq_wtf_attach", columnList = "entry_id,uri")
})
public class WtfEntryAttach {

    private long id;
    private WtfEntry wtfEntry;
    private String attachURI;
    private Timestamp createDate;

    public WtfEntryAttach() {
    }

    @PrePersist
    public void prePersist() {
        if (createDate == null) {
            createDate = Timestamp.from(Instant.now());
        }
    }

    @Id
    @GeneratedValue(generator = "wtf_assigns_attach_ids", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "wtf_assigns_attach_ids", sequenceName = "wtf_assigns_attach_ids")
    @Column(name = "id", nullable = false, unique = true)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entry_id", nullable = false)
    public WtfEntry getWtfEntry() {
        return wtfEntry;
    }

    public void setWtfEntry(WtfEntry wtfEntry) {
        this.wtfEntry = wtfEntry;
    }

    @Column(name = "uri", length = 2000, nullable = false)
    public String getAttachURI() {
        return attachURI;
    }

    public void setAttachURI(String attachURI) {
        this.attachURI = attachURI;
    }

    @Column(name = "create_date", nullable = false)
    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "WtfEntryAttach{" +
                "id=" + id +
                ", attachURI='" + attachURI + '\'' +
                ", createDate=" + createDate +
                '}';
    }
}

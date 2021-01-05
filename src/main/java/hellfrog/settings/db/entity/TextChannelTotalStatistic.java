package hellfrog.settings.db.entity;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "text_channel_total_stats", indexes = {
        @Index(name = "uniq_text_channel_total_stat", columnList = "server_id,text_channel_id,user_id")
})
public class TextChannelTotalStatistic {

    private long id;
    private long serverId;
    private long textChannelId;
    private long userId;
    private long messagesCount;
    private Timestamp lastMessageDate;
    private long symbolsCount;
    private long bytesCount;
    private Timestamp createDate;
    private Timestamp updateDate;

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
    @GeneratedValue(generator = "text_channel_total_stat_idx", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "text_channel_total_stat_idx", sequenceName = "text_channel_total_stat_idx")
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

    @Column(name = "text_channel_id", nullable = false)
    public long getTextChannelId() {
        return textChannelId;
    }

    public void setTextChannelId(long textChannelId) {
        this.textChannelId = textChannelId;
    }

    @Column(name = "user_id")
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Column(name = "messages_count", nullable = false)
    public long getMessagesCount() {
        return messagesCount;
    }

    public void setMessagesCount(long messagesCount) {
        this.messagesCount = messagesCount;
    }

    @Column(name = "last_message_date", nullable = false)
    public Timestamp getLastMessageDate() {
        return lastMessageDate;
    }

    public void setLastMessageDate(Timestamp lastMessageDate) {
        this.lastMessageDate = lastMessageDate;
    }

    @Column(name = "symbols_count", nullable = false)
    public long getSymbolsCount() {
        return symbolsCount;
    }

    public void setSymbolsCount(long symbolsCount) {
        this.symbolsCount = symbolsCount;
    }

    @Column(name = "bytes_count", nullable = false)
    public long getBytesCount() {
        return bytesCount;
    }

    public void setBytesCount(long bytesCount) {
        this.bytesCount = bytesCount;
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
        return "TextChannelUserTotalStatistic{" +
                "id=" + id +
                ", serverId=" + serverId +
                ", textChannelId=" + textChannelId +
                ", userId=" + userId +
                ", messagesCount=" + messagesCount +
                ", lastMessageDate=" + lastMessageDate +
                ", symbolsCount=" + symbolsCount +
                ", bytesCount=" + bytesCount +
                ", createDate=" + createDate +
                ", updateDate=" + updateDate +
                '}';
    }
}

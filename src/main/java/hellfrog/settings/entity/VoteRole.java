package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.InstantPersister;
import hellfrog.settings.db.VoteRolesDAOImpl;

import java.time.Instant;
import java.util.Objects;

@DatabaseTable(tableName = "vote_roles", daoClass = VoteRolesDAOImpl.class)
public class VoteRole {

    public static final String ID_FIELD_NAME = "id";
    public static final String VOTE_ID_FIELD_NAME = "vote_id";
    public static final String MESSAGE_ID_FIELD_NAME = "message_id";
    public static final String ROLE_ID_FIELD_NAME = "role_id";
    public static final String CREATE_DATE_FIELD_NAME = "create_date";

    @DatabaseField(columnName = ID_FIELD_NAME, generatedId = true, canBeNull = false)
    private long id;
    @DatabaseField(columnName = VOTE_ID_FIELD_NAME, canBeNull = false, foreign = true, uniqueIndexName = "uniq_vote_role")
    private ActiveVote activeVote;
    @DatabaseField(columnName = MESSAGE_ID_FIELD_NAME, canBeNull = false, indexName = "vote_roles_msg")
    private long messageId = 0L;
    @DatabaseField(columnName = ROLE_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_vote_role")
    private long roleId;
    @DatabaseField(columnName = CREATE_DATE_FIELD_NAME, canBeNull = false,
            defaultValue = "0", persisterClass = InstantPersister.class)
    private Instant createDate;

    public VoteRole() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ActiveVote getActiveVote() {
        return activeVote;
    }

    public void setActiveVote(ActiveVote activeVote) {
        this.activeVote = activeVote;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
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
        VoteRole voteRole = (VoteRole) o;
        long activeVoteId = activeVote != null ? activeVote.getId() : -1L;
        long thatActiveVoteId = voteRole.activeVote != null ? voteRole.activeVote.getId() : -1L;
        return id == voteRole.id &&
                messageId == voteRole.messageId &&
                roleId == voteRole.roleId &&
                activeVoteId == thatActiveVoteId &&
                Objects.equals(createDate, voteRole.createDate);
    }

    @Override
    public int hashCode() {
        long activeVoteId = activeVote != null ? activeVote.getId() : -1L;
        return Objects.hash(id, activeVoteId, messageId, roleId, createDate);
    }

    @Override
    public String toString() {
        long activeVoteId = activeVote != null ? activeVote.getId() : -1L;
        return "VoteRole{" +
                "id=" + id +
                ", activeVoteId=" + activeVoteId +
                ", messageId=" + messageId +
                ", roleId=" + roleId +
                ", createDate=" + createDate +
                '}';
    }
}

package hellfrog.settings.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import hellfrog.settings.db.InstantPersister;
import hellfrog.settings.db.RoleRightsDAOImpl;

import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

@DatabaseTable(tableName = "role_rights", daoClass = RoleRightsDAOImpl.class)
public class RoleRight
        implements RightEntity {

    public static final String ROLE_ID_FIELD_NAME = "role_id";

    @DatabaseField(generatedId = true, unique = true, columnName = ID_FIELD_NAME, canBeNull = false)
    private long id;
    @DatabaseField(columnName = SERVER_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_role_right")
    private long serverId;
    @DatabaseField(columnName = COMMAND_PREFIX_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_role_right")
    private String commandPrefix;
    @DatabaseField(columnName = ROLE_ID_FIELD_NAME, canBeNull = false, uniqueIndexName = "uniq_role_right")
    private long roleId;
    @DatabaseField(columnName = CREATE_DATE_FIELD_NAME, canBeNull = false, persisterClass = InstantPersister.class)
    private Instant createDate;

    public RoleRight() {
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

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    @Override
    public long getEntityId() {
        return roleId;
    }

    @Override
    public void setEntityId(long newEntityId) {
        this.roleId = newEntityId;
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
        RoleRight roleRight = (RoleRight) o;
        return id == roleRight.id &&
                serverId == roleRight.serverId &&
                roleId == roleRight.roleId &&
                Objects.equals(commandPrefix, roleRight.commandPrefix) &&
                Objects.equals(createDate, roleRight.createDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, serverId, commandPrefix, roleId, createDate);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RoleRight.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("serverId=" + serverId)
                .add("commandPrefix='" + commandPrefix + "'")
                .add("roleId=" + roleId)
                .add("createDate=" + createDate)
                .toString();
    }
}

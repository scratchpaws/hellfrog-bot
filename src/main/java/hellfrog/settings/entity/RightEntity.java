package hellfrog.settings.entity;

import java.time.Instant;

public interface RightEntity {

    String ID_FIELD_NAME = "id";
    String SERVER_ID_FIELD_NAME = "server_id";
    String COMMAND_PREFIX_FIELD_NAME = "command_prefix";
    String CREATE_DATE_FIELD_NAME = "create_date";

    long getId();

    void setId(long id);

    long getServerId();

    void setServerId(long serverId);

    String getCommandPrefix();

    void setCommandPrefix(String commandPrefix);

    long getEntityId();

    void setEntityId(long newEntityId);

    Instant getCreateDate();

    void setCreateDate(Instant createDate);
}

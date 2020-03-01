package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

public class TextChannelRightsDAO extends EntityRightsDAO {

    public TextChannelRightsDAO(@NotNull Connection connection) {
        super(connection, "text_channel_rights", "channel_id");
    }
}

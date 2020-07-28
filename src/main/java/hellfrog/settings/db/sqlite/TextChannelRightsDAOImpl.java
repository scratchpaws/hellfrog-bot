package hellfrog.settings.db.sqlite;

import hellfrog.settings.db.TextChannelRightsDAO;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

class TextChannelRightsDAOImpl
        extends EntityRightsDAO
        implements TextChannelRightsDAO {

    public TextChannelRightsDAOImpl(@NotNull Connection connection) {
        super(connection, "text_channel_rights", "channel_id");
    }
}

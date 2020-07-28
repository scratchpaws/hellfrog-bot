package hellfrog.settings.db.sqlite;

import hellfrog.settings.db.ChannelCategoryRightsDAO;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

class ChannelCategoryRightsDAOImpl
        extends EntityRightsDAO
        implements ChannelCategoryRightsDAO {

    ChannelCategoryRightsDAOImpl(@NotNull Connection connection) {
        super(connection, "category_rights", "category_id");
    }
}

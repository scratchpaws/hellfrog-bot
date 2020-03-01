package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

public class ChannelCategoryRightsDAO extends EntityRightsDAO {

    ChannelCategoryRightsDAO(@NotNull Connection connection) {
        super(connection, "category_rights", "category_id");
    }
}

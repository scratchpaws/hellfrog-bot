package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

public class UserRightsDAO extends EntityRightsDAO {

    public UserRightsDAO(@NotNull Connection connection) {
        super(connection, "user_rights", "user_id");
    }
}

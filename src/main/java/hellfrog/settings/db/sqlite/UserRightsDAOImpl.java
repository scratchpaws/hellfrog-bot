package hellfrog.settings.db.sqlite;

import hellfrog.settings.db.UserRightsDAO;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

class UserRightsDAOImpl
        extends EntityRightsDAO
        implements UserRightsDAO {

    public UserRightsDAOImpl(@NotNull Connection connection) {
        super(connection, "user_rights", "user_id");
    }
}

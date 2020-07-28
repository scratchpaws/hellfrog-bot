package hellfrog.settings.db.sqlite;

import hellfrog.settings.db.RoleRightsDAO;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

class RoleRightsDAOImpl
        extends EntityRightsDAO
        implements RoleRightsDAO {

    public RoleRightsDAOImpl(@NotNull Connection connection) {
        super(connection, "role_rights", "role_id");
    }
}

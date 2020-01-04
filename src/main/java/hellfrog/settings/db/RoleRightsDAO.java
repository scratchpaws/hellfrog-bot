package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;

public class RoleRightsDAO extends EntityRightsDAO {

    public RoleRightsDAO(@NotNull Connection connection) {
        super(connection, "role_rights", "role_id");
    }
}

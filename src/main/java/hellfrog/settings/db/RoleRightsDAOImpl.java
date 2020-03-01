package hellfrog.settings.db;

import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.RoleRight;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class RoleRightsDAOImpl
        extends EntityRightsDAO<RoleRight, Long>
        implements RoleRightsDAO {

    public RoleRightsDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, RoleRight.class);
    }

    @Override
    protected String getEntityFieldIdName() {
        return RoleRight.ROLE_ID_FIELD_NAME;
    }

    @Override
    protected RoleRight createEntity() {
        return new RoleRight();
    }
}

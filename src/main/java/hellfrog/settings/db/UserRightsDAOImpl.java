package hellfrog.settings.db;

import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.UserRight;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class UserRightsDAOImpl
        extends EntityRightsDAO<UserRight, Long>
        implements UserRightsDAO {

    public UserRightsDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, UserRight.class);
    }

    @Override
    protected String getEntityFieldIdName() {
        return UserRight.USER_ID_FIELD_NAME;
    }

    @Override
    protected UserRight createEntity() {
        return new UserRight();
    }
}

package hellfrog.settings.db;

import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.ChannelCategoryRight;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class ChannelCategoryRightsDAOImpl
        extends EntityRightsDAO<ChannelCategoryRight, Long>
        implements ChannelCategoryRightsDAO {

    public ChannelCategoryRightsDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, ChannelCategoryRight.class);
    }

    @Override
    protected String getEntityFieldIdName() {
        return ChannelCategoryRight.CATEGORY_ID_FIELD_NAME;
    }

    @Override
    protected ChannelCategoryRight createEntity() {
        return new ChannelCategoryRight();
    }
}

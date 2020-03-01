package hellfrog.settings.db;

import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.TextChannelRight;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class TextChannelRightsDAOImpl
        extends EntityRightsDAO<TextChannelRight, Long>
        implements TextChannelRightsDAO {
    
    public TextChannelRightsDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, TextChannelRight.class);
    }

    @Override
    protected String getEntityFieldIdName() {
        return TextChannelRight.CHANNEL_ID_FIELD_NAME;
    }

    @Override
    protected TextChannelRight createEntity() {
        return new TextChannelRight();
    }
}

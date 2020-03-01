package hellfrog.settings.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.ActiveVotePoint;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class ActiveVotePointsDAOImpl
        extends BaseDaoImpl<ActiveVotePoint, Long>
        implements ActiveVotePointsDAO {

    public ActiveVotePointsDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, ActiveVotePoint.class);
    }
}

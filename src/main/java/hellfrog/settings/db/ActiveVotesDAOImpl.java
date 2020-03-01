package hellfrog.settings.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.ActiveVote;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class ActiveVotesDAOImpl extends BaseDaoImpl<ActiveVote, Long>
        implements ActiveVotesDAO {

    public ActiveVotesDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, ActiveVote.class);
    }
}

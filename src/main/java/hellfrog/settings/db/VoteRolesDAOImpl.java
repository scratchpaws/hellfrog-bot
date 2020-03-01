package hellfrog.settings.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.VoteRole;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class VoteRolesDAOImpl
        extends BaseDaoImpl<VoteRole, Long>
        implements VoteRolesDAO {

    public VoteRolesDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, VoteRole.class);
    }
}

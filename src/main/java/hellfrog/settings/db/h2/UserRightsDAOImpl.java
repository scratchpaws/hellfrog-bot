package hellfrog.settings.db.h2;

import hellfrog.settings.db.UserRightsDAO;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UserRightsDAOImpl
        implements UserRightsDAO {

    @Override
    public List<Long> getAllAllowed(long serverId, @NotNull String commandPrefix) {
        return null;
    }

    @Override
    public boolean isAllowed(long serverId, long who, @NotNull String commandPrefix) {
        return false;
    }

    @Override
    public boolean allow(long serverId, long who, @NotNull String commandPrefix) {
        return false;
    }

    @Override
    public boolean deny(long serverId, long who, @NotNull String commandPrefix) {
        return false;
    }
}

package hellfrog.settings.db;

import com.j256.ormlite.dao.Dao;
import hellfrog.settings.entity.UserRight;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface UserRightsDAO extends Dao<UserRight, Long> {

    List<Long> getAllAllowed(long serverId, @NotNull String commandPrefix);

    boolean isAllowed(long serverId, long who, @NotNull String commandPrefix);

    boolean allow(long serverId, long who, @NotNull String commandPrefix);

    boolean deny(long serverId, long who, @NotNull String commandPrefix);
}

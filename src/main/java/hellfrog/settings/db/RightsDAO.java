package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface RightsDAO {

    List<Long> getAllAllowed(long serverId, @NotNull String commandPrefix);

    long getAllowedCount(long serverId, @NotNull String commandPrefix);

    boolean isAllowed(long serverId, long who, @NotNull String commandPrefix);

    boolean allow(long serverId, long who, @NotNull String commandPrefix);

    boolean deny(long serverId, long who, @NotNull String commandPrefix);
}

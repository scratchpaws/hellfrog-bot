package hellfrog.settings.db;

import hellfrog.settings.db.entity.RoleAssign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.Instant;
import java.util.List;

public interface RoleAssignDAO {

    void addToQueue(final long serverId, final long userId, final long roleId, @NotNull final Instant assignTime);

    void clearQueueFor(final long serverId, final long userId);

    @UnmodifiableView
    @NotNull
    List<RoleAssign> getQueueFor(final long serverId);

    @UnmodifiableView
    @NotNull
    List<RoleAssign> getTimeoutReached(final long serverId, @NotNull final Instant time);

    @NotNull
    @UnmodifiableView
    List<Long> getQueueServerList();

    @UnmodifiableView
    @NotNull
    List<RoleAssign> getTimeoutReached(final long serverId);

    void discardRoleAssign(@NotNull final RoleAssign roleAssign);
}

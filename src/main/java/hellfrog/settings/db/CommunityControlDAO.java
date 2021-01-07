package hellfrog.settings.db;

import hellfrog.settings.db.entity.CommunityControlSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.Optional;

public interface CommunityControlDAO {

    @NotNull
    Optional<CommunityControlSettings> getSettings(final long serverId);

    void setSettings(@NotNull final CommunityControlSettings settings);

    @NotNull
    @UnmodifiableView List<Long> getUsers(final long serverId);

    void addUser(final long serverId, final long userId);

    void removeUser(final long serverId, final long userId);
}

package hellfrog.settings.db;

import hellfrog.settings.db.entity.EntityNameCache;
import hellfrog.settings.db.entity.NameType;
import hellfrog.settings.db.entity.ServerNameCache;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface EntityNameCacheDAO {

    Optional<EntityNameCache> find(long entityId);

    void update(long entityId, @NotNull final String entityName, @NotNull final NameType nameType);

    void update(@NotNull final EntityNameCache entityNameCache);

    Optional<ServerNameCache> find(long serverId, long entityId);

    void update(long serverId, long entityId, @NotNull final String entityName);

    void update(@NotNull final ServerNameCache nameCache);
}

package hellfrog.settings.db;

import hellfrog.settings.db.entity.EntityNameCache;
import hellfrog.settings.db.entity.NameType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface EntityNameCacheDAO {

    Optional<EntityNameCache> find(long entityId);

    void update(long entityId, @NotNull final String entityName, @NotNull final NameType nameType);

    void update(@NotNull final EntityNameCache entityNameCache);
}

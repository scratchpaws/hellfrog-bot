package hellfrog.settings.db;

import hellfrog.settings.db.entity.WtfEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface WtfAssignDAO {

    Optional<WtfEntry> getLatest(long serverId, long userId);

    List<WtfEntry> getAll(long serverId, long userId);

    AddUpdateState addOrUpdate(@NotNull WtfEntry wtfEntry);

    AddUpdateState remove(long serverId, long authorId, long userId);
}

package hellfrog.settings.db;

import hellfrog.settings.db.entity.AutoPromoteConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;

public interface AutoPromoteRolesDAO {

    @UnmodifiableView
    @NotNull
    List<AutoPromoteConfig> loadAllConfigs(final long serverId);

    void addUpdateConfig(final long serverId, final long roleId, final long timeoutSeconds);

    void deleteConfig(final long serverId, final long roleId);
}

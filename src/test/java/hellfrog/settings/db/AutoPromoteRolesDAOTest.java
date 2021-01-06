package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.settings.db.entity.AutoPromoteConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoPromoteRolesDAOTest {

    @Test
    public void promotesTest() throws Exception {

        List<ServerEntity> serversList = buildServerList();

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            AutoPromoteRolesDAO autoPromoteRolesDAO = mainDBController.getAutoPromoteRolesDAO();

            serversList.parallelStream().forEach(serverEntity -> {
                List<AutoPromoteConfig> all = autoPromoteRolesDAO.loadAllConfigs(serverEntity.serverId);
                TestUtils.assertEmpty(all, "Initial value must be empty");
                serverEntity.initialConfigs.parallelStream().forEach(initialValue -> {
                    autoPromoteRolesDAO.addConfig(serverEntity.serverId, initialValue.roleId, initialValue.timeout);
                    List<AutoPromoteConfig> savedList = autoPromoteRolesDAO.loadAllConfigs(serverEntity.serverId);
                    boolean found = false;
                    for (AutoPromoteConfig config : savedList) {
                        if (config.getRoleId() == initialValue.roleId
                                && config.getServerId() == serverEntity.serverId
                                && config.getTimeout() == initialValue.timeout) {
                            found = true;
                            break;
                        }
                    }
                    Assertions.assertTrue(found, "Initial value must be present");
                });
                serverEntity.toDelete.parallelStream().forEach(deleteValue -> {
                    autoPromoteRolesDAO.deleteConfig(serverEntity.serverId, deleteValue.roleId);
                    List<AutoPromoteConfig> savedList = autoPromoteRolesDAO.loadAllConfigs(serverEntity.serverId);
                    boolean found = false;
                    for (AutoPromoteConfig config : savedList) {
                        if (config.getRoleId() == deleteValue.roleId
                                && config.getServerId() == serverEntity.serverId) {
                            found = true;
                            break;
                        }
                    }
                    Assertions.assertFalse(found, "Deleted value must not be present");
                });
            });
        }
    }

    private List<ServerEntity> buildServerList() {
        List<ServerEntity> serversList = new ArrayList<>();
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        for (int i = 1; i <= tlr.nextInt(2, 5); i++) {
            serversList.add(new ServerEntity());
        }
        return Collections.unmodifiableList(serversList);
    }

    private static class ServerEntity {

        private final long serverId = TestUtils.randomDiscordEntityId();
        private final List<PromoteConfig> initialConfigs;
        private final List<PromoteConfig> toDelete;

        ServerEntity() {
            List<PromoteConfig> _initial = new ArrayList<>();
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            int count = tlr.nextInt(3, 5);
            for (int i = 1; i <= count; i++) {
                _initial.add(new PromoteConfig());
            }
            initialConfigs = Collections.unmodifiableList(_initial);
            toDelete = TestUtils.randomSublist(initialConfigs, 1, 3);
        }
    }

    private static class PromoteConfig {
        private final long roleId = TestUtils.randomDiscordEntityId();
        private final long timeout = ThreadLocalRandom.current().nextLong(0L, 3600L);
    }
}

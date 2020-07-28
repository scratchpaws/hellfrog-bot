package hellfrog.settings.db;

import hellfrog.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ServerPreferencesDAOTest {

    @Test
    public void testValues() throws Exception {

        Map<Long, ServerSettings> servers = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            while (true) {
                long serverId = TestUtils.randomDiscordEntityId();
                if (!servers.containsKey(serverId)) {
                    ServerSettings sset = new ServerSettings();
                    servers.put(serverId, sset);
                    break;
                }
            }
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            ServerPreferencesDAO preferencesDAO = mainDBController.getServerPreferencesDAO();

            String awaitPrefix = ServerPreferencesDAO.PREFIX_DEFAULT;
            boolean awaitJoinLeaveDisplay = ServerPreferencesDAO.JOIN_LEAVE_DISPLAY_DEFAULT;
            long awaitJoinLeaveChannelId = ServerPreferencesDAO.JOIN_LEAVE_CHANNEL_ID_DEFAULT;
            boolean awaitNewAclMode = ServerPreferencesDAO.NEW_ACL_MODE_DEFAULT;

            // извлечение значений по-умолчанию
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();

                String prefix = preferencesDAO.getPrefix(serverId);
                boolean joinLeaveDisplay = preferencesDAO.isJoinLeaveDisplay(serverId);
                long joinLeaveChannel = preferencesDAO.getJoinLeaveChannel(serverId);
                boolean newAclMode = preferencesDAO.isNewAclMode(serverId);

                Assertions.assertEquals(awaitPrefix, prefix);
                Assertions.assertEquals(awaitJoinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(awaitJoinLeaveChannelId, joinLeaveChannel);
                Assertions.assertEquals(awaitNewAclMode, newAclMode);
            }

            // задание значений, здесь извлекаются всё ещё значения по-умолчанию
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();
                ServerSettings settings = entry.getValue();

                String prefix = preferencesDAO.setPrefix(serverId, settings.prefix);
                boolean joinLeaveDisplay = preferencesDAO.setJoinLeaveDisplay(serverId, settings.joinLeaveDisplay);
                long joinLeaveChannel = preferencesDAO.setJoinLeaveChannel(serverId, settings.joinLeaveChannel);
                boolean newAclMode = preferencesDAO.setNewAclMode(serverId, settings.newAclMode);

                Assertions.assertEquals(awaitPrefix, prefix);
                Assertions.assertEquals(awaitJoinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(awaitJoinLeaveChannelId, joinLeaveChannel);
                Assertions.assertEquals(awaitNewAclMode, newAclMode);
            }

            // проверяем, что значения сохранились
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();
                ServerSettings settings = entry.getValue();

                String prefix = preferencesDAO.getPrefix(serverId);
                boolean joinLeaveDisplay = preferencesDAO.isJoinLeaveDisplay(serverId);
                long joinLeaveChannel = preferencesDAO.getJoinLeaveChannel(serverId);
                boolean newAclMode = preferencesDAO.isNewAclMode(serverId);

                Assertions.assertEquals(settings.prefix, prefix);
                Assertions.assertEquals(settings.joinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(settings.joinLeaveChannel, joinLeaveChannel);
                Assertions.assertEquals(settings.newAclMode, newAclMode);
            }

            // проверяем, что значения не перезаписались дефолтом при извлечении данных
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();
                ServerSettings settings = entry.getValue();

                String prefix = preferencesDAO.getPrefix(serverId);
                boolean joinLeaveDisplay = preferencesDAO.isJoinLeaveDisplay(serverId);
                long joinLeaveChannel = preferencesDAO.getJoinLeaveChannel(serverId);
                boolean newAclMode = preferencesDAO.isNewAclMode(serverId);

                Assertions.assertEquals(settings.prefix, prefix);
                Assertions.assertEquals(settings.joinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(settings.joinLeaveChannel, joinLeaveChannel);
                Assertions.assertEquals(settings.newAclMode, newAclMode);
            }
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            ServerPreferencesDAO preferencesDAO = mainDBController.getServerPreferencesDAO();

            // проверяем, что значения сохранились после закрытия БД
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();
                ServerSettings settings = entry.getValue();

                String prefix = preferencesDAO.getPrefix(serverId);
                boolean joinLeaveDisplay = preferencesDAO.isJoinLeaveDisplay(serverId);
                long joinLeaveChannel = preferencesDAO.getJoinLeaveChannel(serverId);
                boolean newAclMode = preferencesDAO.isNewAclMode(serverId);

                Assertions.assertEquals(settings.prefix, prefix);
                Assertions.assertEquals(settings.joinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(settings.joinLeaveChannel, joinLeaveChannel);
                Assertions.assertEquals(settings.newAclMode, newAclMode);
            }
        }
    }

    private static class ServerSettings {

        final String prefix;
        final boolean joinLeaveDisplay;
        final long joinLeaveChannel;
        final boolean newAclMode;

        ServerSettings() {
            ThreadLocalRandom lr = ThreadLocalRandom.current();
            prefix = TestUtils.randomStringName(3);
            joinLeaveDisplay = lr.nextBoolean();
            joinLeaveChannel = TestUtils.randomDiscordEntityId();
            newAclMode = lr.nextBoolean();
        }
    }
}

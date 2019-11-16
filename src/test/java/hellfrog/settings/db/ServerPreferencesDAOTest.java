package hellfrog.settings.db;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ServerPreferencesDAOTest {

    private static final Path SETTINGS_PATH = Paths.get("./settings/");
    private static final String TEST_DB_NAME = "test.sqlite3";
    private static final Path tstBase = SETTINGS_PATH.resolve(TEST_DB_NAME);

    @Test
    public void testValues() throws Exception {

        Map<Long, ServerSettings> servers = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            while (true) {
                long serverId = genEntityId();
                if (!servers.containsKey(serverId)) {
                    ServerSettings sset = new ServerSettings();
                    servers.put(serverId, sset);
                    break;
                }
            }
        }

        Files.deleteIfExists(tstBase);
        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
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

        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
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

    private static long genEntityId() {
        return ThreadLocalRandom.current()
                .nextLong(199999999999999999L, 999999999999999999L);
    }

    private static class ServerSettings {

        final String prefix;
        final boolean joinLeaveDisplay;
        final long joinLeaveChannel;
        final boolean newAclMode;

        ServerSettings() {
            ThreadLocalRandom lr = ThreadLocalRandom.current();
            StringBuilder prefBuilder = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                int plus = lr.nextInt(0, 25);
                prefBuilder.append((char)('a' + plus));
            }
            prefix = prefBuilder.toString();
            joinLeaveDisplay = lr.nextBoolean();
            joinLeaveChannel = genEntityId();
            newAclMode = lr.nextBoolean();
        }
    }
}

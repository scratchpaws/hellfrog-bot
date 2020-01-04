package hellfrog.settings.db;

import hellfrog.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class RightsDAOTest {

    private static final Path SETTINGS_PATH = Paths.get("./settings/");
    private static final String TEST_DB_NAME = "test.sqlite3";
    private static final Path tstBase = SETTINGS_PATH.resolve(TEST_DB_NAME);

    private static final List<Long> USERS_LIST = TestUtils.randomDiscordEntitiesIds(10, 100);

    @Test
    public void testRightsEntities() throws Exception {

        List<TestServer> testServers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            while (true) {
                TestServer testServer = new TestServer();
                boolean exists = false;
                for (TestServer item : testServers) {
                    if (item.serverId == testServer.serverId) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    testServers.add(testServer);
                    break;
                }
            }
        }

        Files.deleteIfExists(tstBase);
        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            UserRightsDAO userRightsDAO = mainDBController.getUserRightsDAO();
            RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
            TextChannelRightsDAO textChannelRightsDAO = mainDBController.getTextChannelRightsDAO();
            ChannelCategoryRightsDAO channelCategoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();

            testServers.parallelStream()
                    .forEach(testServer -> testServer.testRights.keySet().parallelStream().forEach(cmd -> {
                        TestCommandRights rights = testServer.testRights.get(cmd);
                        rights.allowedUsers
                                .parallelStream()
                                .forEach(userId -> {
                                    boolean isAllowed = userRightsDAO.isAllowed(testServer.serverId,
                                            userId, cmd);
                                    Assertions.assertFalse(isAllowed);
                                    boolean isAdded = userRightsDAO.allow(testServer.serverId, userId, cmd);
                                    Assertions.assertTrue(isAdded);
                                    isAllowed = userRightsDAO.isAllowed(testServer.serverId, userId, cmd);
                                    Assertions.assertTrue(isAllowed);
                                });
                        rights.allowedRoles
                                .parallelStream()
                                .forEach(roleId -> {
                                    boolean isAllowed = roleRightsDAO.isAllowed(testServer.serverId, roleId,
                                            cmd);
                                    Assertions.assertFalse(isAllowed);
                                    boolean isAdded = roleRightsDAO.allow(testServer.serverId, roleId, cmd);
                                    Assertions.assertTrue(isAdded);
                                    isAllowed = roleRightsDAO.isAllowed(testServer.serverId, roleId, cmd);
                                    Assertions.assertTrue(isAllowed);
                                });
                        rights.allowedTextChats
                                .parallelStream()
                                .forEach(textChatId -> {
                                    boolean isAllowed = textChannelRightsDAO.isAllowed(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertFalse(isAllowed);
                                    boolean isAdded = textChannelRightsDAO.allow(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertTrue(isAdded);
                                    isAllowed = textChannelRightsDAO.isAllowed(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertTrue(isAllowed);
                                });
                        rights.allowedCategories
                                .parallelStream()
                                .forEach(categoryId -> {
                                    boolean isAllowed = channelCategoryRightsDAO.isAllowed(testServer.serverId,
                                            categoryId, cmd);
                                    Assertions.assertFalse(isAllowed);
                                    boolean isAdded = channelCategoryRightsDAO.allow(testServer.serverId,
                                            categoryId, cmd);
                                    Assertions.assertTrue(isAdded);
                                    isAllowed = channelCategoryRightsDAO.isAllowed(testServer.serverId,
                                            categoryId, cmd);
                                    Assertions.assertTrue(isAllowed);
                                });
                        Assertions.assertEquals(rights.allowedUsers.size(),
                                userRightsDAO.getAllAllowed(testServer.serverId, cmd).size());
                        Assertions.assertEquals(rights.allowedRoles.size(),
                                roleRightsDAO.getAllAllowed(testServer.serverId, cmd).size());
                        Assertions.assertEquals(rights.allowedTextChats.size(),
                                textChannelRightsDAO.getAllAllowed(testServer.serverId, cmd).size());
                        Assertions.assertEquals(rights.allowedCategories.size(),
                                channelCategoryRightsDAO.getAllAllowed(testServer.serverId, cmd).size());
                    }));
        }

        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            UserRightsDAO userRightsDAO = mainDBController.getUserRightsDAO();
            RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
            TextChannelRightsDAO textChannelRightsDAO = mainDBController.getTextChannelRightsDAO();
            ChannelCategoryRightsDAO channelCategoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();

            testServers.parallelStream()
                    .forEach(testServer -> testServer.testRights.keySet().parallelStream().forEach(cmd -> {
                        TestCommandRights rights = testServer.testRights.get(cmd);
                        rights.allowedUsers
                                .parallelStream()
                                .forEach(userId -> {
                                    boolean isAllowed = userRightsDAO.isAllowed(testServer.serverId,
                                            userId, cmd);
                                    Assertions.assertTrue(isAllowed);
                                    boolean isDeleted = userRightsDAO.deny(testServer.serverId, userId, cmd);
                                    Assertions.assertTrue(isDeleted);
                                    isAllowed = userRightsDAO.isAllowed(testServer.serverId, userId, cmd);
                                    Assertions.assertFalse(isAllowed);
                                });
                        rights.allowedRoles
                                .parallelStream()
                                .forEach(roleId -> {
                                    boolean isAllowed = roleRightsDAO.isAllowed(testServer.serverId, roleId,
                                            cmd);
                                    Assertions.assertTrue(isAllowed);
                                    boolean isDeleted = roleRightsDAO.deny(testServer.serverId, roleId, cmd);
                                    Assertions.assertTrue(isDeleted);
                                    isAllowed = roleRightsDAO.isAllowed(testServer.serverId, roleId, cmd);
                                    Assertions.assertFalse(isAllowed);
                                });
                        rights.allowedTextChats
                                .parallelStream()
                                .forEach(textChatId -> {
                                    boolean isAllowed = textChannelRightsDAO.isAllowed(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertTrue(isAllowed);
                                    boolean isDeleted = textChannelRightsDAO.deny(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertTrue(isDeleted);
                                    isAllowed = textChannelRightsDAO.isAllowed(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertFalse(isAllowed);
                                });
                        rights.allowedCategories
                                .parallelStream()
                                .forEach(categoryId -> {
                                    boolean isAllowed = channelCategoryRightsDAO.isAllowed(testServer.serverId,
                                            categoryId, cmd);
                                    Assertions.assertTrue(isAllowed);
                                    boolean isDeleted = channelCategoryRightsDAO.deny(testServer.serverId,
                                            categoryId, cmd);
                                    Assertions.assertTrue(isDeleted);
                                    isAllowed = channelCategoryRightsDAO.isAllowed(testServer.serverId,
                                            categoryId, cmd);
                                    Assertions.assertFalse(isAllowed);
                                });
                        Assertions.assertTrue(userRightsDAO.getAllAllowed(testServer.serverId, cmd).isEmpty());
                        Assertions.assertTrue(roleRightsDAO.getAllAllowed(testServer.serverId, cmd).isEmpty());
                        Assertions.assertTrue(textChannelRightsDAO.getAllAllowed(testServer.serverId, cmd)
                                .isEmpty());
                        Assertions.assertTrue(channelCategoryRightsDAO.getAllAllowed(testServer.serverId, cmd)
                                .isEmpty());
                    }));
        }
    }

    private static class TestServer {
        final long serverId = TestUtils.randomDiscordEntityId();
        final Map<String, TestCommandRights> testRights;

        TestServer() {
            Map<String, TestCommandRights> temp = new HashMap<>();
            List<String> keys = TestUtils.randomNames(3, 5, 5);
            for (String key : keys) {
                temp.put(key, new TestCommandRights());
            }
            testRights = Collections.unmodifiableMap(temp);
        }
    }

    private static class TestCommandRights {
        final List<Long> allowedUsers = TestUtils.randomSublist(USERS_LIST, 10, USERS_LIST.size() / 2);
        final List<Long> allowedRoles = TestUtils.randomDiscordEntitiesIds(10);
        final List<Long> allowedTextChats = TestUtils.randomDiscordEntitiesIds(50);
        final List<Long> allowedCategories = TestUtils.randomDiscordEntitiesIds(10);
    }
}

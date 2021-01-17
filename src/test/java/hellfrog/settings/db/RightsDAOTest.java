package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.core.LogsStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class RightsDAOTest {

    private static final List<Long> USERS_LIST = TestUtils.randomDiscordEntitiesIds(10, 100);

    @Test
    public void testRightsEntities() throws Exception {

        List<TestServer> testServers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
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

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            UserRightsDAO userRightsDAO = mainDBController.getUserRightsDAO();
            RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
            ChannelRightsDAO channelRightsDAO = mainDBController.getChannelRightsDAO();
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
                                    boolean isAllowed = channelRightsDAO.isAllowed(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertFalse(isAllowed);
                                    boolean isAdded = channelRightsDAO.allow(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertTrue(isAdded);
                                    isAllowed = channelRightsDAO.isAllowed(testServer.serverId,
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
                                channelRightsDAO.getAllAllowed(testServer.serverId, cmd).size());
                        Assertions.assertEquals(rights.allowedCategories.size(),
                                channelCategoryRightsDAO.getAllAllowed(testServer.serverId, cmd).size());
                    }));
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            UserRightsDAO userRightsDAO = mainDBController.getUserRightsDAO();
            RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
            ChannelRightsDAO channelRightsDAO = mainDBController.getChannelRightsDAO();
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
                                    boolean isAllowed = channelRightsDAO.isAllowed(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertTrue(isAllowed);
                                    boolean isDeleted = channelRightsDAO.deny(testServer.serverId,
                                            textChatId, cmd);
                                    Assertions.assertTrue(isDeleted);
                                    isAllowed = channelRightsDAO.isAllowed(testServer.serverId,
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
                        Assertions.assertTrue(channelRightsDAO.getAllAllowed(testServer.serverId, cmd)
                                .isEmpty());
                        Assertions.assertTrue(channelCategoryRightsDAO.getAllAllowed(testServer.serverId, cmd)
                                .isEmpty());
                    }));
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
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
        final List<Long> allowedUsers = TestUtils.randomSublist(USERS_LIST, 5, USERS_LIST.size() / 2);
        final List<Long> allowedRoles = TestUtils.randomDiscordEntitiesIds(5);
        final List<Long> allowedTextChats = TestUtils.randomDiscordEntitiesIds(5);
        final List<Long> allowedCategories = TestUtils.randomDiscordEntitiesIds(2);
    }
}

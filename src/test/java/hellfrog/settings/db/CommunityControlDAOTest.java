package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.common.CommonUtils;
import hellfrog.core.LogsStorage;
import hellfrog.settings.db.entity.CommunityControlSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CommunityControlDAOTest {

    @Test
    public void testSettings() throws Exception {

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            CommunityControlDAO communityControlDAO = mainDBController.getCommunityControlDAO();

            List<TestServer> testServers = buildTestServers();

            testServers.parallelStream().forEach(testServer -> {
                Optional<CommunityControlSettings> settings = communityControlDAO.getSettings(testServer.serverId);
                Assertions.assertTrue(settings.isEmpty(), "Server settings must be missing");
                testServer.settingsChanges.forEach(change -> {
                    CommunityControlSettings override = new CommunityControlSettings();
                    override.setServerId(testServer.serverId);
                    override.setRoleId(change.roleId);
                    override.setThreshold(change.threshold);
                    override.setCustomEmojiId(change.customEmojiId);
                    override.setUnicodeEmoji(change.unicodeEmoji);
                    communityControlDAO.setSettings(override);
                    Optional<CommunityControlSettings> mayBeChanged = communityControlDAO.getSettings(testServer.serverId);
                    Assertions.assertTrue(mayBeChanged.isPresent(), "Override settings must be present: "
                            + printChange(testServer.serverId, change));
                });
            });
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    @Test
    public void testUsers() throws Exception {

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            CommunityControlDAO communityControlDAO = mainDBController.getCommunityControlDAO();

            List<TestServer> testServers = buildTestServers();
            testServers.parallelStream().forEach(testServer -> {

                List<Long> afterRemove = testServer.controlUsers.stream()
                        .filter(userId -> !testServer.toDelete.contains(userId))
                        .collect(Collectors.toUnmodifiableList());

                List<Long> mustBeEmpty = communityControlDAO.getUsers(testServer.serverId);
                TestUtils.assertEmpty(mustBeEmpty, "Community control users must be empty");

                testServer.controlUsers.forEach(userId -> communityControlDAO.addUser(testServer.serverId, userId));
                List<Long> addedList = communityControlDAO.getUsers(testServer.serverId);

                testServer.toDelete.forEach(userId -> communityControlDAO.removeUser(testServer.serverId, userId));
                List<Long> afterRemoveList = communityControlDAO.getUsers(testServer.serverId);

                String debugAddedList = printUsers(addedList);
                String debugAfterRemoveList = printUsers(afterRemoveList);

                String debugToAddList = printUsers(testServer.controlUsers);

                boolean containsAdded = addedList.containsAll(testServer.controlUsers);
                Assertions.assertTrue(containsAdded, "Community control users must contain all added:\n" +
                        debugToAddList + "\nBut contains:\n" + debugAddedList);

                for (long userId : afterRemove) {
                    boolean pass = afterRemoveList.contains(userId);
                    Assertions.assertTrue(pass, "Community control user " + userId + " must be present into " +
                            "list after removal users from remove list.\n" +
                            "After remove list:\n" + debugAfterRemoveList);
                }

                for (long userId : testServer.toDelete) {
                    boolean pass = afterRemoveList.contains(userId);
                    Assertions.assertFalse(pass, "Removed community control user " + userId + " must not be " +
                            "present into list after removal users from remove list.\n" +
                            "After remove list:\n" + debugAfterRemoveList);
                }
            });
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    private String printChange(final long serverId, final @NotNull Settings change) {
        return String.format("{Change:serverId:%d,roleId:%d,threshold:%d,unicodeEmoji:'%s',customEmojiId:%d}",
                serverId, change.roleId, change.threshold, change.unicodeEmoji, change.customEmojiId);
    }

    private String printUsers(@Nullable final List<Long> users) {
        return users == null || users.isEmpty()
                ? "{Users list:\nEMPTY\n}"
                : "{Users list:\n" + users.stream()
                .map(String::valueOf)
                .map(s -> "[" + s + "]")
                .reduce(CommonUtils::reduceNewLine)
                .orElse("EMPTY") + "\n}";
    }

    @NotNull @UnmodifiableView
    private List<TestServer> buildTestServers() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        int serversCount = tlr.nextInt(3, 5);
        List<TestServer> result = new ArrayList<>(serversCount);
        for (int i = 1; i <= serversCount; i++) {
            result.add(new TestServer());
        }
        return Collections.unmodifiableList(result);
    }

    private static class TestServer {
        private final long serverId = TestUtils.randomDiscordEntityId();
        private final List<Settings> settingsChanges;
        private final List<Long> controlUsers = TestUtils.randomDiscordEntitiesIds(5, 10);
        private final List<Long> toDelete = TestUtils.randomSublist(controlUsers, 1, 3);

        TestServer() {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            int changesCount = tlr.nextInt(3, 5);
            List<Settings> _settingsChanges = new ArrayList<>(changesCount);
            for (int i = 1; i <= changesCount; i++) {
                _settingsChanges.add(new Settings());
            }
            settingsChanges = Collections.unmodifiableList(_settingsChanges);
        }
    }

    private static class Settings {

        private final long roleId = TestUtils.randomDiscordEntityId();
        private final long threshold;
        private final String unicodeEmoji;
        private final long customEmojiId;

        Settings() {
            ThreadLocalRandom tlr = ThreadLocalRandom.current();
            threshold = tlr.nextLong(2L, 10L);
            if (tlr.nextBoolean()) {
                unicodeEmoji = null;
                customEmojiId = TestUtils.randomDiscordEntityId();
            } else {
                unicodeEmoji = TestUtils.randomStringName(12);
                customEmojiId = 0L;
            }
        }
    }
}

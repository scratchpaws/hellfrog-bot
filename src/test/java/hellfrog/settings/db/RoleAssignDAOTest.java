package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.common.CommonUtils;
import hellfrog.core.LogsStorage;
import hellfrog.settings.db.entity.RoleAssign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RoleAssignDAOTest {

    @Test
    public void emptyQueuePresenceTest() throws Exception {

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            RoleAssignDAO roleAssignDAO = mainDBController.getRoleAssignDAO();

            TestUtils.randomDiscordEntitiesIds(5, 10).parallelStream().forEach(serverId -> {
                List<RoleAssign> queue = roleAssignDAO.getQueueFor(serverId);
                TestUtils.assertEmpty(queue, "Initial queue must be empty");
                queue = roleAssignDAO.getTimeoutReached(serverId);
                TestUtils.assertEmpty(queue, "Initial queue must be empty");
                queue = roleAssignDAO.getTimeoutReached(serverId, Instant.EPOCH);
                TestUtils.assertEmpty(queue, "Initial queue must be empty");
            });
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    @Test
    public void emptyQueueClearTest() throws Exception {
        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            RoleAssignDAO roleAssignDAO = mainDBController.getRoleAssignDAO();

            roleAssignDAO.clearQueueFor(TestUtils.randomDiscordEntityId(), TestUtils.randomDiscordEntityId());
        }
        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    @Test
    public void additionalTest() throws Exception {

        final List<TestServer> testServers = buildTestServers();

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            RoleAssignDAO roleAssignDAO = mainDBController.getRoleAssignDAO();

            testServers.parallelStream().forEach(testServer -> testServer.roleAssigns
                    .parallelStream().forEach(testRoleAssign ->
                            roleAssignDAO.addToQueue(testServer.serverId, testRoleAssign.userId, testRoleAssign.roleId, testRoleAssign.assignDate)));

            for (TestServer testServer : testServers) {
                // All added items
                List<RoleAssign> queue = roleAssignDAO.getQueueFor(testServer.serverId);
                // The queue of expired role assignments. This method extracts them from the database while deleting them from the database
                List<RoleAssign> expired = roleAssignDAO.getTimeoutReached(testServer.serverId, testServer.dateTime);
                // Repeated call of the method should return an empty queue, since a previous call to the same method should have cleared the queue
                List<RoleAssign> emptyAfterExpired = roleAssignDAO.getTimeoutReached(testServer.serverId, testServer.dateTime);
                // Queue after extracting expired records. Future role assignments should persist
                List<RoleAssign> queueAfterExpired = roleAssignDAO.getQueueFor(testServer.serverId);

                String debugQueue = formatQueue(queue);
                String debugExpiredQueue = formatQueue(expired);
                String debugEmptyAfterExpired = formatQueue(emptyAfterExpired);
                String debugQueueAfterExpired = formatQueue(queueAfterExpired);

                TestUtils.assertNotEmpty(queue, "Queue must not be empty");
                TestUtils.assertNotEmpty(expired, "Timeout reached queue must not be empty");
                TestUtils.assertEmpty(emptyAfterExpired, "Timeout reached queue after same method call must be empty" +
                        debugEmptyAfterExpired);
                TestUtils.assertNotEmpty(queueAfterExpired, "Queue after expired wipe must not be empty");

                for (TestRoleAssign assign : testServer.roleAssigns) {
                    String debugAssign = formatAssign(assign);
                    boolean isPresent = present(testServer.serverId, queue, assign);
                    Assertions.assertTrue(isPresent, "Assign must be present into queue:\n" + debugAssign
                            + "\nInitial queue:\n" + debugQueue);
                    isPresent = present(testServer.serverId, expired, assign);
                    if (!assign.future && !isPresent) {
                        Assertions.fail("Expired assign must be present into timeout reached queue:\n" + debugAssign
                                + "\nTimeout reached queue:\n" + debugExpiredQueue + "\nInitial queue:\n" + debugQueue);
                    } else if (assign.future && isPresent) {
                        Assertions.fail("Future assign must NOT be present into timeout reached queue:\n" + debugAssign
                                + "\nTimeout reached queue:\n" + debugExpiredQueue);
                    }
                    isPresent = present(testServer.serverId, queueAfterExpired, assign);
                    if (!assign.future && isPresent) {
                        Assertions.fail("Expired assign must not be present into queue that fetch after expired method call:\n" + debugAssign
                                + "\nQueue:\n" + debugQueueAfterExpired);
                    } else if (assign.future && !isPresent) {
                        Assertions.fail("Future assign must be present into queue that fetch after expired method call:\n" + debugAssign
                                + "\nQueue:\n" + debugQueueAfterExpired + "\nInitial queue:\n" + debugQueue);
                    }
                }
            }
        }
        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    @Test
    public void removalTest() throws Exception {

        final List<TestServer> testServers = buildTestServers();

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            RoleAssignDAO roleAssignDAO = mainDBController.getRoleAssignDAO();

            testServers.parallelStream().forEach(testServer -> testServer.roleAssigns
                    .parallelStream().forEach(testRoleAssign ->
                            roleAssignDAO.addToQueue(testServer.serverId, testRoleAssign.userId, testRoleAssign.roleId, testRoleAssign.assignDate)));

            boolean cleared = false;
            boolean discarded = false;
            for (TestServer testServer : testServers) {
                // All added items
                List<RoleAssign> queue = roleAssignDAO.getQueueFor(testServer.serverId);
                String debugQueue = formatQueue(queue);
                TestUtils.assertNotEmpty(queue, "Queue must not be empty");

                if (!cleared) {
                    testServer.roleAssigns.parallelStream().forEach(assign ->
                            roleAssignDAO.clearQueueFor(testServer.serverId, assign.userId));
                    cleared = true;
                    List<RoleAssign> afterClear = roleAssignDAO.getQueueFor(testServer.serverId);
                    String debugAfterClear = formatQueue(afterClear);
                    for (TestRoleAssign assign : testServer.roleAssigns) {
                        boolean isPresent = present(testServer.serverId, afterClear, assign);
                        if (isPresent) {
                            Assertions.fail("Assign must not be present into queue after clear: "
                                    + formatAssign(assign) + "\nQueue after clear:\n" + debugAfterClear);
                        }
                    }
                    continue;
                }

                if (!discarded) {
                    queue.forEach(roleAssignDAO::discardRoleAssign);
                    discarded = true;
                    List<RoleAssign> afterDiscard = roleAssignDAO.getQueueFor(testServer.serverId);
                    String debugAfterDiscard = formatQueue(afterDiscard);
                    TestUtils.assertEmpty(afterDiscard, "Assigns queue must be empty after discard " +
                            "all items\nAfter discard queue:\n" + debugAfterDiscard);
                    continue;
                }

                for (TestRoleAssign assign : testServer.roleAssigns) {
                    String debugAssign = formatAssign(assign);
                    boolean isPresent = present(testServer.serverId, queue, assign);
                    Assertions.assertTrue(isPresent, "Assign must be present into queue:\n" + debugAssign
                            + "\nInitial queue:\n" + debugQueue);
                }
            }
        }
        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    private String formatAssign(@Nullable final TestRoleAssign assign) {
        if (assign == null) {
            return "{Assign:null}";
        }
        return "{Assign:r:" + assign.roleId + ",u:" + assign.userId + ",t:" + assign.assignDate + "}";
    }

    private String formatQueue(@Nullable final List<RoleAssign> queue) {
        if (queue == null || queue.isEmpty()) {
            return "{Queue:\nempty\n}";
        }
        return "{Queue:\n" + queue.stream()
                .map(a -> "  [r:" + a.getRoleId()
                        + ",u:" + a.getUserId()
                        + ",t:" + formatTimestamp(a.getAssignDate())
                        + "]")
                .reduce(CommonUtils::reduceNewLine)
                .orElse("empty\n")
                + "}";
    }

    private boolean present(long serverId, @NotNull final List<RoleAssign> queue, @NotNull final TestRoleAssign assign) {
        boolean found = false;
        for (RoleAssign item : queue) {
            if (item.getServerId() == serverId
                    && item.getRoleId() == assign.roleId
                    && item.getUserId() == assign.userId
                    && formatTimestamp(item.getAssignDate()).equals(formatInstant(assign.assignDate))) {
                found = true;
                break;
            }
        }
        return found;
    }

    private String formatTimestamp(@NotNull final Timestamp value) {
        return formatInstant(value.toInstant());
    }

    private String formatInstant(@NotNull final Instant value) {
        Calendar calendar = CommonUtils.instantToCalendar(value);
        return String.format("%tF %<tT", calendar);
    }

    @NotNull
    @UnmodifiableView
    private List<TestServer> buildTestServers() {
        List<TestServer> result = new ArrayList<>();
        int count = ThreadLocalRandom.current().nextInt(2, 5);
        for (int i = 1; i <= count; i++) {
            result.add(new TestServer());
        }
        return Collections.unmodifiableList(result);
    }

    private static class TestServer {
        private final long serverId = TestUtils.randomDiscordEntityId();
        private final List<TestRoleAssign> roleAssigns;
        private final Instant dateTime = Instant.now();

        TestServer() {
            int countFuture = ThreadLocalRandom.current().nextInt(3, 5);
            int countExpired = ThreadLocalRandom.current().nextInt(3, 5);
            List<TestRoleAssign> _roleAssigns = new ArrayList<>(countFuture);
            for (int i = 1; i <= countFuture; i++) {
                _roleAssigns.add(new TestRoleAssign(true));
            }
            for (int i = 1; i <= countExpired; i++) {
                _roleAssigns.add(new TestRoleAssign(false));
            }
            this.roleAssigns = Collections.unmodifiableList(_roleAssigns);
        }
    }

    private static class TestRoleAssign {
        private final long userId = TestUtils.randomDiscordEntityId();
        private final long roleId = TestUtils.randomDiscordEntityId();
        private final Instant assignDate;
        private final boolean future;

        TestRoleAssign(boolean future) {
            this.future = future;
            if (future) {
                assignDate = Instant.now()
                        .plusSeconds(ThreadLocalRandom.current().nextLong(10L, 3600L));
            } else {
                assignDate = Instant.now()
                        .minusSeconds(ThreadLocalRandom.current().nextLong(0L, 3600L));
            }
        }
    }
}

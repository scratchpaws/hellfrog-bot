package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.core.LogsStorage;
import hellfrog.settings.db.entity.WtfEntry;
import hellfrog.settings.db.entity.WtfEntryAttach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WtfAssignDAOTest {

    private static final int MIN_MEMBERS_COUNT = 5;
    private static final int MAX_MEMBERS_COUNT = 15;
    private static final int MAX_AUTHORS = 5;

    @Test
    public void testValues() throws Exception {

        List<TestServer> testServers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            testServers.add(new TestServer());
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            WtfAssignDAO wtfAssignDAO = mainDBController.getWtfAssignDAO();

            testServers.parallelStream()
                    .forEach(testServer -> {
                        // Addition test
                        testServer.firstMap.parallelStream().forEach(wtfMap -> {
                            AddUpdateState state = wtfAssignDAO.addOrUpdate(wtfMap);
                            Assertions.assertEquals(AddUpdateState.ADDED, state);
                            Optional<WtfEntry> mayBeEntry = wtfAssignDAO.getLatest(testServer.serverId, wtfMap.getTargetId());
                            Assertions.assertTrue(mayBeEntry.isPresent());
                        });
                        // Test presence
                        testServer.firstMap.parallelStream().forEach(wtfMap -> {
                            List<WtfEntry> found = wtfAssignDAO.getAll(testServer.serverId, wtfMap.getTargetId());
                            Assertions.assertTrue(checkPresent(wtfMap, found));
                        });
                        // Test update
                        testServer.firstUpgrade.parallelStream().forEach(wtfMap -> {
                            AddUpdateState state = wtfAssignDAO.addOrUpdate(wtfMap);
                            Assertions.assertEquals(AddUpdateState.UPDATED, state);
                            List<WtfEntry> found = wtfAssignDAO.getAll(testServer.serverId, wtfMap.getTargetId());
                            String messages = "Search:\n" + wtfMap + "\nIn:\n" + found;
                            Assertions.assertTrue(checkPresent(wtfMap, found), messages);
                        });
                        // Test second update
                        testServer.secondUpgrade.parallelStream().forEach(wtfMap -> {
                            AddUpdateState state = wtfAssignDAO.addOrUpdate(wtfMap);
                            Assertions.assertEquals(AddUpdateState.UPDATED, state);
                            List<WtfEntry> found = wtfAssignDAO.getAll(testServer.serverId, wtfMap.getTargetId());
                            String messages = "Search:\n" + wtfMap + "\nIn:\n" + found;
                            Assertions.assertTrue(checkPresent(wtfMap, found), messages);
                        });
                        // Deletion test
                        testServer.deletion.parallelStream().forEach(wtfMap -> {
                            AddUpdateState state = wtfAssignDAO.remove(testServer.serverId,
                                    wtfMap.getAuthorId(), wtfMap.getTargetId());
                            Assertions.assertEquals(AddUpdateState.REMOVED, state);
                            List<WtfEntry> found = wtfAssignDAO.getAll(testServer.serverId, wtfMap.getTargetId());
                            Assertions.assertFalse(checkPresent(wtfMap, found));
                        });
                    });
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    private boolean checkEquals(@Nullable WtfEntry first, @Nullable WtfEntry second) {
        if (first == null || second == null) return false;
        boolean equalsServers = first.getServerId() == second.getServerId();
        boolean equalsAuthors = first.getAuthorId() == second.getAuthorId();
        boolean equalsTargets = first.getTargetId() == second.getTargetId();
        boolean equalsDescription = first.getDescription() == null && second.getDescription() == null
                || (first.getDescription() != null && second.getDescription() != null
                && first.getDescription().equals(second.getDescription()));
        boolean equalsURI = checkEquals(first.getAttaches(), second.getAttaches());
        return equalsDescription && equalsURI && equalsAuthors && equalsTargets && equalsServers;
    }

    private boolean checkEquals(@Nullable Set<WtfEntryAttach> first, @Nullable Set<WtfEntryAttach> second) {
        if (isEmptySet(first) && isEmptySet(second)) return true;
        if (isEmptySet(first) || isEmptySet(second)) return false;
        for (WtfEntryAttach firstAttach : first) {
            boolean found = false;
            for (WtfEntryAttach secondAttach : second) {
                if (checkEquals(firstAttach, secondAttach)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private boolean isEmptySet(@Nullable Set<WtfEntryAttach> set) {
        return set == null || set.isEmpty();
    }

    private boolean checkEquals(@Nullable WtfEntryAttach first, @Nullable WtfEntryAttach second) {
        if (first == null || second == null) return false;
        return first.getAttachURI() != null && second.getAttachURI() != null
                && first.getAttachURI().equals(second.getAttachURI());
    }

    private boolean checkPresent(@NotNull WtfEntry first, @NotNull List<WtfEntry> list) {
        for (WtfEntry entry : list) {
            if (checkEquals(first, entry)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    static WtfEntry buildEntry(long serverId, long author, long member) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        String description = null;
        if (tlr.nextBoolean()) {
            description = TestUtils.randomStringName(50);
        }
        WtfEntry wtfEntry = new WtfEntry();
        if (tlr.nextBoolean()) {
            wtfEntry.setId(TestUtils.randomDiscordEntityId());
        }
        wtfEntry.setServerId(serverId);
        wtfEntry.setAuthorId(author);
        wtfEntry.setTargetId(member);
        wtfEntry.setDescription(description);
        Timestamp now = Timestamp.from(Instant.now());
        wtfEntry.setCreateDate(now);
        wtfEntry.setUpdateDate(now);
        if (tlr.nextBoolean() || wtfEntry.getDescription() == null) {
            int attachesCount = tlr.nextInt(1, 3);
            Set<WtfEntryAttach> attaches = new HashSet<>();
            for (int i = 0; i < attachesCount; i++) {
                WtfEntryAttach entryAttach = new WtfEntryAttach();
                if (tlr.nextBoolean()) {
                    entryAttach.setWtfEntry(wtfEntry);
                }
                if (tlr.nextBoolean()) {
                    entryAttach.setId(TestUtils.randomDiscordEntityId());
                }
                entryAttach.setAttachURI(TestUtils.randomURI().toString());
                attaches.add(entryAttach);
            }
            wtfEntry.setAttaches(attaches);
        }
        return wtfEntry;
    }

    @NotNull
    @UnmodifiableView
    private static List<WtfEntry> generateWtfEntries(final long serverId,
                                                     @NotNull List<Long> members,
                                                     @NotNull List<Long> authors) {
        List<WtfEntry> result = new ArrayList<>();
        for (long author : authors) {
            List<Long> targets = TestUtils.randomSublist(members, MIN_MEMBERS_COUNT, members.size());
            for (long member : targets) {
                WtfEntry wtfMap = buildEntry(serverId, author, member);
                result.add(wtfMap);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @NotNull
    @UnmodifiableView
    private static List<WtfEntry> generateUpdate(@NotNull List<WtfEntry> previousList) {
        List<WtfEntry> subList = TestUtils.randomSublist(previousList, previousList.size() > 0 ? 1 : 0, previousList.size());
        List<WtfEntry> result = new ArrayList<>();
        for (WtfEntry wtfMap : subList) {
            WtfEntry upgradedEntry = buildEntry(wtfMap.getServerId(), wtfMap.getAuthorId(), wtfMap.getTargetId());
            result.add(upgradedEntry);
        }
        return Collections.unmodifiableList(result);
    }

    public static class TestServer {
        final long serverId = TestUtils.randomDiscordEntityId();
        final List<Long> members = TestUtils.randomDiscordEntitiesIds(MIN_MEMBERS_COUNT, MAX_MEMBERS_COUNT);
        final List<Long> authors = TestUtils.randomSublist(members, 1, MAX_AUTHORS);
        final List<WtfEntry> firstMap = generateWtfEntries(serverId, members, authors);
        final List<WtfEntry> firstUpgrade = generateUpdate(firstMap);
        final List<WtfEntry> secondUpgrade = generateUpdate(firstMap);
        final List<WtfEntry> deletion = TestUtils.randomSublist(firstMap, firstMap.size() > 0 ? 1 : 0, firstMap.size());
    }
}

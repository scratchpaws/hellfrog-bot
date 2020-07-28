package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.settings.db.entity.WtfEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
                            AddUpdateState state = wtfAssignDAO.addOrUpdate(testServer.serverId,
                                    wtfMap.member, wtfMap.wtfEntry);
                            Assertions.assertEquals(AddUpdateState.ADDED, state);
                            Optional<WtfEntry> mayBeEntry = wtfAssignDAO.getLatest(testServer.serverId, wtfMap.member);
                            Assertions.assertTrue(mayBeEntry.isPresent());
                        });
                        // Test presence
                        testServer.firstMap.parallelStream().forEach(wtfMap -> {
                            List<WtfEntry> found = wtfAssignDAO.getAll(testServer.serverId, wtfMap.member);
                            Assertions.assertTrue(checkPresent(wtfMap.wtfEntry, found));
                        });
                        // Test update
                        testServer.firstUpgrade.parallelStream().forEach(wtfMap -> {
                            AddUpdateState state = wtfAssignDAO.addOrUpdate(testServer.serverId,
                                    wtfMap.member, wtfMap.wtfEntry);
                            Assertions.assertEquals(AddUpdateState.UPDATED, state);
                            List<WtfEntry> found = wtfAssignDAO.getAll(testServer.serverId, wtfMap.member);
                            Assertions.assertTrue(checkPresent(wtfMap.wtfEntry, found));
                        });
                        // Test second update
                        testServer.secondUpgrade.parallelStream().forEach(wtfMap -> {
                            AddUpdateState state = wtfAssignDAO.addOrUpdate(testServer.serverId,
                                    wtfMap.member, wtfMap.wtfEntry);
                            Assertions.assertEquals(AddUpdateState.UPDATED, state);
                            List<WtfEntry> found = wtfAssignDAO.getAll(testServer.serverId, wtfMap.member);
                            Assertions.assertTrue(checkPresent(wtfMap.wtfEntry, found));
                        });
                        // Deletion test
                        testServer.deletion.parallelStream().forEach(wtfMap -> {
                            AddUpdateState state = wtfAssignDAO.remove(testServer.serverId,
                                    wtfMap.wtfEntry.getAuthorId(), wtfMap.member);
                            Assertions.assertEquals(AddUpdateState.REMOVED, state);
                            List<WtfEntry> found = wtfAssignDAO.getAll(testServer.serverId, wtfMap.member);
                            Assertions.assertFalse(checkPresent(wtfMap.wtfEntry, found));
                        });
                    });
        }
    }

    private boolean checkEquals(@Nullable WtfEntry first, @Nullable WtfEntry second) {
        if (first == null || second == null) return false;
        boolean equalsDescription = first.getDescription() == null && second.getDescription() == null
                || (first.getDescription() != null && second.getDescription() != null
                && first.getDescription().equals(second.getDescription()));
        boolean equalsURI = first.getUri() == null && second.getUri() == null
                || (first.getUri() != null && second.getUri() != null
                && first.getUri().equals(second.getUri()));
        boolean equalsAuthors = first.getAuthorId() == second.getAuthorId();
        return equalsDescription && equalsURI && equalsAuthors;
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
    static WtfEntry buildEntry(long author) {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        String description = null;
        if (tlr.nextBoolean()) {
            description = TestUtils.randomStringName(50);
        }
        URI uri = null;
        if (tlr.nextBoolean() || description == null) {
            uri = TestUtils.randomURI();
        }
        return WtfEntry.newBuilder()
                .description(description)
                .uri(uri)
                .authorId(author)
                .date(Instant.now())
                .build();
    }

    @NotNull
    static WtfEntry upgradeEntry(@NotNull WtfEntry entry) {
        WtfEntry update = buildEntry(entry.getAuthorId());
        return entry.toBuilder()
                .description(update.getDescription())
                .uri(update.getUri())
                .date(update.getDate())
                .build();
    }

    static class WtfMap {

        final long member;
        final WtfEntry wtfEntry;

        WtfMap(long member, @NotNull WtfEntry wtfEntry) {
            this.member = member;
            this.wtfEntry = wtfEntry;
        }
    }

    @NotNull
    @UnmodifiableView
    private static List<WtfMap> generateWtfMaps(@NotNull List<Long> members,
                                                @NotNull List<Long> authors) {
        List<WtfMap> result = new ArrayList<>();
        for (long author : authors) {
            List<Long> targets = TestUtils.randomSublist(members, MIN_MEMBERS_COUNT, members.size());
            for (long member : targets) {
                WtfMap wtfMap = new WtfMap(member, buildEntry(author));
                result.add(wtfMap);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @NotNull
    @UnmodifiableView
    private static List<WtfMap> generateUpdate(@NotNull List<WtfMap> previousList) {
        List<WtfMap> subList = TestUtils.randomSublist(previousList, previousList.size() > 0 ? 1 : 0, previousList.size());
        List<WtfMap> result = new ArrayList<>();
        for (WtfMap wtfMap : subList) {
            WtfEntry upgradedEntry = upgradeEntry(wtfMap.wtfEntry);
            WtfMap upgradedMap = new WtfMap(wtfMap.member, upgradedEntry);
            result.add(upgradedMap);
        }
        return Collections.unmodifiableList(result);
    }

    public static class TestServer {
        final long serverId = TestUtils.randomDiscordEntityId();
        final List<Long> members = TestUtils.randomDiscordEntitiesIds(MIN_MEMBERS_COUNT, MAX_MEMBERS_COUNT);
        final List<Long> authors = TestUtils.randomSublist(members, 1, MAX_AUTHORS);
        final List<WtfMap> firstMap = generateWtfMaps(members, authors);
        final List<WtfMap> firstUpgrade = generateUpdate(firstMap);
        final List<WtfMap> secondUpgrade = generateUpdate(firstMap);
        final List<WtfMap> deletion = TestUtils.randomSublist(firstMap, firstMap.size() > 0 ? 1 : 0, firstMap.size());
    }
}

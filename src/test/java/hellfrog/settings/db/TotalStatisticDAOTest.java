package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.common.CommonConstants;
import hellfrog.settings.db.entity.EmojiTotalStatistic;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TotalStatisticDAOTest
        implements CommonConstants {

    @Test
    public void testValues() throws Exception {
        ThreadLocalRandom tl = ThreadLocalRandom.current();
        int count = tl.nextInt(5, 10);

        final List<TestServer> testServers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            testServers.add(new TestServer());
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            TotalStatisticDAO statisticDAO = mainDBController.getTotalStatisticDAO();

            // проверяем, что изначально отдаются пустые значения
            for (TestServer testServer : testServers) {
                List<EmojiTotalStatistic> total = statisticDAO.getEmojiUsagesStatistic(testServer.serverId);
                Assertions.assertTrue(total.isEmpty(), "Initial values empty check");
            }
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            TotalStatisticDAO statisticDAO = mainDBController.getTotalStatisticDAO();

            // Проверка инициирующего инкремента и прогона до инициирующего значения
            testServers.parallelStream().forEach(testServer -> testServer.testEmojis.parallelStream().forEach(testEmoji -> {
                for (long l = 1L; l <= testEmoji.changesPath.initialValue; l++) {
                    statisticDAO.incrementEmoji(testServer.serverId, testEmoji.emojiId);
                }
            }));

            for (TestServer testServer : testServers) {
                List<EmojiTotalStatistic> total = statisticDAO.getEmojiUsagesStatistic(testServer.serverId);
                for (TestEmoji testEmoji : testServer.testEmojis) {
                    boolean found = false;
                    for (EmojiTotalStatistic statistic : total) {
                        if (statistic.getEmojiId() == testEmoji.emojiId) {
                            found = true;
                            Assertions.assertEquals(testEmoji.changesPath.initialValue, statistic.getUsagesCount(), "Initial increment value check");
                            break;
                        }
                    }
                    Assertions.assertTrue(found, "Initial increment presence check");
                }
            }
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            TotalStatisticDAO statisticDAO = mainDBController.getTotalStatisticDAO();

            // Многопоточный прогон изменений
            testServers.parallelStream().forEach(testServer -> testServer.testEmojis.parallelStream().forEach(testEmoji -> {
                statisticDAO.insertEmojiStats(testServer.serverId, testEmoji.emojiId, testEmoji.changesPath.initialValue, Instant.now());
                testEmoji.changesPath.changes.forEach(increment -> {
                    if (increment) {
                        statisticDAO.incrementEmoji(testServer.serverId, testEmoji.emojiId);
                    } else {
                        statisticDAO.decrementEmoji(testServer.serverId, testEmoji.emojiId);
                    }
                });
            }));
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            TotalStatisticDAO statisticDAO = mainDBController.getTotalStatisticDAO();

            // проверяем, что статистика рассчитана корректно
            for (TestServer testServer : testServers) {
                List<EmojiTotalStatistic> total = statisticDAO.getEmojiUsagesStatistic(testServer.serverId);
                for (TestEmoji testEmoji : testServer.testEmojis) {
                    boolean found = false;
                    for (EmojiTotalStatistic statistic : total) {
                        if (statistic.getEmojiId() == testEmoji.emojiId) {
                            found = true;
                            Assertions.assertEquals(testEmoji.changesPath.awaitValues, statistic.getUsagesCount(), testEmoji.changesPath.debugLog);
                            break;
                        }
                    }
                    Assertions.assertTrue(found, "After updates presence check");
                }
            }
        }
    }

    private static class TestServer {

        final long serverId = TestUtils.randomDiscordEntityId();
        final List<TestEmoji> testEmojis;

        TestServer() {
            ThreadLocalRandom tl = ThreadLocalRandom.current();
            int count = tl.nextInt(5, 10);
            List<TestEmoji> emojiList = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                emojiList.add(new TestEmoji());
            }
            testEmojis = Collections.unmodifiableList(emojiList);
        }
    }

    private static class TestEmoji {

        final long emojiId = TestUtils.randomDiscordEntityId();
        final ChangesPath changesPath = new ChangesPath();

    }

    private static class ChangesPath {

        final long initialValue;
        final BooleanList changes;
        final long awaitValues;
        final String debugLog;

        ChangesPath() {
            ThreadLocalRandom tl = ThreadLocalRandom.current();
            initialValue = tl.nextLong(0L, 10000L);
            int changesCount = tl.nextInt(5, 50);
            MutableBooleanList changesPath = new BooleanArrayList(changesCount);
            for (int i = 0; i < changesCount; i++) {
                changesPath.add(tl.nextBoolean());
            }
            changes = changesPath.toImmutable();
            StringBuilder changesLog = new StringBuilder("Changes check: ");
            long awaitingValue = initialValue;
            changesLog.append(initialValue);
            BooleanIterator iterator = changes.booleanIterator();
            while (iterator.hasNext()) {
                boolean increment = iterator.next();
                if (increment) {
                    awaitingValue++;
                    changesLog.append('+');
                } else {
                    awaitingValue = awaitingValue > 0L ? awaitingValue - 1 : 0L;
                    changesLog.append('-');
                }
            }
            awaitValues = awaitingValue;
            changesLog.append(awaitValues);
            debugLog = changesLog.toString();
        }
    }
}

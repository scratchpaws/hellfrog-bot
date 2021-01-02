package hellfrog.settings.db;

import hellfrog.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EmojiTotalStatisticDAOTest {

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
            EmojiTotalStatisticDAO emojiDAO = mainDBController.getEmojiTotalStatisticDAO();

            // проверяем, что изначально отдаются пустые значения
            for (TestServer testServer : testServers) {
                for (TestEmoji testEmoji : testServer.testEmojis) {
                    long usagesCount = emojiDAO.getEmojiUsagesCount(testServer.serverId, testEmoji.emojiId);
                    Assertions.assertEquals(EmojiTotalStatisticDAO.NO_USAGES_FOUND, usagesCount);
                }
            }
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            EmojiTotalStatisticDAO emojiDAO = mainDBController.getEmojiTotalStatisticDAO();

            // Многопоточный прогон изменений
            testServers.parallelStream().forEach(testServer -> testServer.testEmojis.parallelStream().forEach(testEmoji -> {
                emojiDAO.insertStats(testServer.serverId, testEmoji.emojiId, testEmoji.initialValue, Instant.now());
                for (boolean increment : testEmoji.changes) {
                    if (increment) {
                        emojiDAO.increment(testServer.serverId, testEmoji.emojiId);
                    } else {
                        emojiDAO.decrement(testServer.serverId, testEmoji.emojiId);
                    }
                }
            }));
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            EmojiTotalStatisticDAO emojiDAO = mainDBController.getEmojiTotalStatisticDAO();

            // проверяем, что статистика рассчитана корректно
            for (TestServer testServer : testServers) {
                for (TestEmoji testEmoji : testServer.testEmojis) {
                    long result = emojiDAO.getEmojiUsagesCount(testServer.serverId, testEmoji.emojiId);
                    Assertions.assertEquals(testEmoji.await, result, testEmoji.debugLog);
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
        final long initialValue;
        final List<Boolean> changes;
        final long await;
        final String debugLog;

        TestEmoji() {
            ThreadLocalRandom tl = ThreadLocalRandom.current();
            initialValue = tl.nextLong(0L, 10000L);
            List<Boolean> changesPath = new ArrayList<>();
            int changesCount = tl.nextInt(5, 50);
            for (int i = 0; i < changesCount; i++) {
                changesPath.add(tl.nextBoolean());
            }
            changes = Collections.unmodifiableList(changesPath);
            StringBuilder changesLog = new StringBuilder();
            long awaitingValue = initialValue;
            changesLog.append(initialValue);
            for (boolean increment : changes) {
                if (increment) {
                    awaitingValue++;
                    changesLog.append('+');
                } else {
                    awaitingValue = awaitingValue > 0L ? awaitingValue - 1 : 0L;
                    changesLog.append('-');
                }
            }
            await = awaitingValue;
            changesLog.append(await);
            debugLog = changesLog.toString();
        }
    }
}

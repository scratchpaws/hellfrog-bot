package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.core.LogsStorage;
import hellfrog.settings.db.entity.EmojiTotalStatistic;
import hellfrog.settings.db.entity.TextChannelTotalStatistic;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.MutableBooleanList;
import org.eclipse.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
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
                List<EmojiTotalStatistic> emojiStats = statisticDAO.getEmojiUsagesStatistic(testServer.serverId);
                Assertions.assertTrue(emojiStats.isEmpty(), "Initial values empty check");
                List<TextChannelTotalStatistic> channelStats = statisticDAO.getChannelsStatistics(testServer.serverId);
                Assertions.assertTrue(channelStats.isEmpty(), "Initial values empty check");
            }
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            TotalStatisticDAO statisticDAO = mainDBController.getTotalStatisticDAO();

            // Проверка инициирующего инкремента и прогона до инициирующего значения
            testServers.parallelStream().forEach(testServer -> {
                testServer.testEmojis.parallelStream().forEach(testEmoji -> {
                    for (long l = 1L; l <= testEmoji.changesPath.initialValue; l++) {
                        statisticDAO.incrementEmoji(testServer.serverId, testEmoji.emojiId);
                    }
                });
                testServer.testChannels.parallelStream().forEach(testChannel -> {
                    testChannel.initialValues.forEach(initialValue -> {
                        for (long l = 1L; l <= initialValue.messagesCount; l++) {
                            if (l < initialValue.messagesCount) {
                                statisticDAO.incrementChannelStats(testServer.serverId, testChannel.channelId, initialValue.userId, 1, 1L);
                            } else {
                                int symbols = (int) (initialValue.symbolsCount - initialValue.messagesCount + 1L);
                                long bytes = initialValue.bytesCount - initialValue.messagesCount + 1L;
                                statisticDAO.incrementChannelStats(testServer.serverId, testChannel.channelId, initialValue.userId,
                                        symbols, bytes);
                            }
                        }
                        if (initialValue.messagesCount == 0L) {
                            statisticDAO.decrementChannelStats(testServer.serverId, testChannel.channelId, initialValue.userId, 1, 1L);
                        }
                    });
                });
            });

            for (TestServer testServer : testServers) {
                List<EmojiTotalStatistic> emojiTotal = statisticDAO.getEmojiUsagesStatistic(testServer.serverId);
                for (TestEmoji testEmoji : testServer.testEmojis) {
                    boolean found = false;
                    for (EmojiTotalStatistic statistic : emojiTotal) {
                        if (statistic.getEmojiId() == testEmoji.emojiId) {
                            found = true;
                            Assertions.assertEquals(testEmoji.changesPath.initialValue, statistic.getUsagesCount(), "Initial increment value check");
                            break;
                        }
                    }
                    Assertions.assertTrue(found, "Initial increment presence check");
                }
                List<TextChannelTotalStatistic> channelTotal = statisticDAO.getChannelsStatistics(testServer.serverId);
                String entitiesList = channelTotal.stream()
                        .map(stats -> "[c:" + stats.getTextChannelId() + ",u:" + stats.getUserId() + "]")
                        .distinct()
                        .reduce(CommonUtils::reduceNewLine)
                        .map(s -> "{server " + testServer.serverId + ", stats:\n" + s + "\n}")
                        .orElse("{server " + testServer.serverId + ", stats: empty}");
                for (TestChannel testChannel : testServer.testChannels) {
                    for (StatisticValue initialValue : testChannel.initialValues) {
                        boolean found = false;
                        for (TextChannelTotalStatistic channelStat : channelTotal) {
                            if (channelStat.getServerId() == testServer.serverId
                                    && channelStat.getTextChannelId() == testChannel.channelId
                                    && channelStat.getUserId() == initialValue.userId) {
                                found = true;
                                String message = String.format("Initial increment value check, server id: %d, channel id: %d, user id: %d\n",
                                        testServer.serverId, testChannel.channelId, initialValue.userId);
                                Assertions.assertEquals(initialValue.messagesCount, channelStat.getMessagesCount(), message
                                        + "Messages count equality.");
                                Assertions.assertEquals(initialValue.symbolsCount, channelStat.getSymbolsCount(), message
                                        + "Symbols count equality.");
                                Assertions.assertEquals(initialValue.bytesCount, channelStat.getBytesCount(), message
                                        + "Bytes count equality");
                                break;
                            }
                        }
                        String message = String.format("Initial increment presence check, server id: %d, channel id: %d, user id: %d\n" +
                                        "Entities list: %s",
                                testServer.serverId, testChannel.channelId, initialValue.userId, entitiesList);
                        Assertions.assertTrue(found, message);
                    }
                }
            }
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            TotalStatisticDAO statisticDAO = mainDBController.getTotalStatisticDAO();

            // Многопоточный прогон изменений
            testServers.parallelStream().forEach(testServer -> {
                testServer.testEmojis.parallelStream().forEach(testEmoji -> {
                    statisticDAO.insertEmojiStats(testServer.serverId, testEmoji.emojiId, testEmoji.changesPath.initialValue, Instant.now());
                    testEmoji.changesPath.changes.forEach(increment -> {
                        if (increment) {
                            statisticDAO.incrementEmoji(testServer.serverId, testEmoji.emojiId);
                        } else {
                            statisticDAO.decrementEmoji(testServer.serverId, testEmoji.emojiId);
                        }
                    });
                });
                testServer.testChannels.parallelStream().forEach(testChannel -> {
                    testChannel.initialValues.forEach(initialValue -> {
                        statisticDAO.insertChannelStats(testServer.serverId, testChannel.channelId, initialValue.userId,
                                initialValue.messagesCount, Instant.now(), initialValue.symbolsCount, initialValue.bytesCount);
                    });
                    testChannel.changesPaths.forEach(change -> {
                        if (change.increment) {
                            statisticDAO.incrementChannelStats(testServer.serverId, testChannel.channelId, change.userId,
                                    change.messageLength, change.bytesCount);
                        } else {
                            statisticDAO.decrementChannelStats(testServer.serverId, testChannel.channelId, change.userId,
                                    change.messageLength, change.bytesCount);
                        }
                    });
                });
            });
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            TotalStatisticDAO statisticDAO = mainDBController.getTotalStatisticDAO();

            // проверяем, что статистика рассчитана корректно
            for (TestServer testServer : testServers) {
                List<EmojiTotalStatistic> emojiTotal = statisticDAO.getEmojiUsagesStatistic(testServer.serverId);
                for (TestEmoji testEmoji : testServer.testEmojis) {
                    boolean found = false;
                    for (EmojiTotalStatistic statistic : emojiTotal) {
                        if (statistic.getEmojiId() == testEmoji.emojiId) {
                            found = true;
                            Assertions.assertEquals(testEmoji.changesPath.awaitValues, statistic.getUsagesCount(), testEmoji.changesPath.debugLog);
                            break;
                        }
                    }
                    Assertions.assertTrue(found, "After updates presence check");
                }
                List<TextChannelTotalStatistic> channelTotal = statisticDAO.getChannelsStatistics(testServer.serverId);
                String entitiesList = channelTotal.stream()
                        .map(stats -> "[c:" + stats.getTextChannelId() + ",u:" + stats.getUserId() + "]")
                        .distinct()
                        .reduce(CommonUtils::reduceNewLine)
                        .map(s -> "{server " + testServer.serverId + ", stats:\n" + s + "\n}")
                        .orElse("{server " + testServer.serverId + ", stats: empty}");
                for (TestChannel testChannel : testServer.testChannels) {
                    for (StatisticValue awaitValue : testChannel.awaitValues) {
                        boolean found = false;
                        for (TextChannelTotalStatistic channelStat : channelTotal) {
                            if (channelStat.getServerId() == testServer.serverId
                                    && channelStat.getTextChannelId() == testChannel.channelId
                                    && channelStat.getUserId() == awaitValue.userId) {
                                found = true;
                                String message = String.format("Statistic validation, server id: %d, channel id: %d, user id: %d\n",
                                        testServer.serverId, testChannel.channelId, awaitValue.userId);
                                Assertions.assertEquals(awaitValue.messagesCount, channelStat.getMessagesCount(), message
                                        + "Messages count equality. Debug: " + awaitValue.debugLog);
                                Assertions.assertEquals(awaitValue.symbolsCount, channelStat.getSymbolsCount(), message
                                        + "Symbols count equality. Debug: " + awaitValue.debugLog);
                                Assertions.assertEquals(awaitValue.bytesCount, channelStat.getBytesCount(), message
                                        + "Bytes count equality. Debug: " + awaitValue.debugLog);
                                break;
                            }
                        }
                        String message = String.format("Statistic validation presence check, server id: %d, channel id: %d, user id: %d\n" +
                                        "Entities list: %s",
                                testServer.serverId, testChannel.channelId, awaitValue.userId, entitiesList);
                        Assertions.assertTrue(found, message);
                    }
                }
            }
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    private static class TestServer {

        final long serverId = TestUtils.randomDiscordEntityId();
        final List<TestEmoji> testEmojis;
        final List<TestChannel> testChannels;

        TestServer() {
            ThreadLocalRandom tl = ThreadLocalRandom.current();
            int count = tl.nextInt(5, 10);
            List<TestEmoji> _testEmojis = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                _testEmojis.add(new TestEmoji());
            }
            testEmojis = Collections.unmodifiableList(_testEmojis);
            final List<Long> testUsers = TestUtils.randomDiscordEntitiesIds(2, 5);
            final List<Long> testChannelIds = TestUtils.randomDiscordEntitiesIds(2, 5);
            List<TestChannel> _testChannels = new ArrayList<>();
            for (long channelId : testChannelIds) {
                _testChannels.add(new TestChannel(channelId, testUsers));
            }
            testChannels = Collections.unmodifiableList(_testChannels);
        }
    }

    private static class TestChannel {

        final long channelId;
        final List<StatisticValue> initialValues;
        final List<StatisticChange> changesPaths;
        final List<StatisticValue> awaitValues;

        TestChannel(final long channelId, final @NotNull List<Long> testUsers) {
            ThreadLocalRandom tl = ThreadLocalRandom.current();
            this.channelId = channelId;
            List<StatisticValue> _initialValues = new ArrayList<>();
            List<StatisticChange> _changesPaths = new ArrayList<>();
            List<StatisticValue> _awaitValues = new ArrayList<>();
            for (long userId : testUsers) {
                StatisticValue initialValue = new StatisticValue(userId);
                _initialValues.add(initialValue);
                int changes = tl.nextInt(2, 5);
                long messages = initialValue.messagesCount;
                long symbols = initialValue.symbolsCount;
                long bytes = initialValue.bytesCount;
                StringBuilder debugLog = new StringBuilder()
                        .append("{Text channel: ").append(channelId)
                        .append(", user: ").append(userId)
                        .append(", start values: [")
                        .append("messages: ").append(messages)
                        .append(", symbols: ").append(symbols)
                        .append(", bytes: ").append(bytes)
                        .append("], changes: [");
                for (int i = 1; i <= changes; i++) {
                    StatisticChange change = new StatisticChange(channelId, userId);
                    _changesPaths.add(change);
                    if (change.increment) {
                        messages++;
                        symbols += change.messageLength;
                        bytes += change.bytesCount;
                        debugLog.append('+')
                                .append("s ").append(change.messageLength)
                                .append("b ").append(change.bytesCount);
                    } else {
                        messages = Math.max(0L, messages - 1L);
                        symbols = Math.max(0L, symbols - change.messageLength);
                        bytes = Math.max(0L, bytes - change.bytesCount);
                        debugLog.append('-')
                                .append("s ").append(change.messageLength)
                                .append("b ").append(change.bytesCount);
                    }
                    if (i < changes) {
                        debugLog.append("; ");
                    }
                }
                debugLog.append("]}\n");
                _awaitValues.add(new StatisticValue(userId, messages, symbols, bytes, debugLog.toString()));
            }
            initialValues = Collections.unmodifiableList(_initialValues);
            changesPaths = Collections.unmodifiableList(_changesPaths);
            awaitValues = Collections.unmodifiableList(_awaitValues);
        }
    }

    private static class StatisticValue {

        final long userId;
        final long messagesCount;
        final long symbolsCount;
        final long bytesCount;
        final String debugLog;

        StatisticValue(final long userId) {
            this.userId = userId;
            ThreadLocalRandom tl = ThreadLocalRandom.current();
            messagesCount = tl.nextLong(0, 10L);
            long _symbols = 0L;
            long _bytes = 0L;
            for (long i = 0; i < messagesCount; i++) {
                _symbols += tl.nextLong(0L, 2001L);
                _bytes += tl.nextLong(0L, CommonConstants.MAX_FILE_SIZE);
            }
            symbolsCount = _symbols;
            bytesCount = _bytes;
            debugLog = "";
        }

        StatisticValue(final long userId, long messagesCount, long symbolsCount, long bytesCount,
                       final String debugLog) {
            this.userId = userId;
            this.messagesCount = messagesCount;
            this.symbolsCount = symbolsCount;
            this.bytesCount = bytesCount;
            this.debugLog = debugLog;
        }
    }

    private static class StatisticChange {

        final long textChannelId;
        final long userId;
        final boolean increment;
        final int messageLength;
        final long bytesCount;

        StatisticChange(final long textChannelId, final long userId) {
            this.textChannelId = textChannelId;
            this.userId = userId;
            ThreadLocalRandom tl = ThreadLocalRandom.current();
            increment = tl.nextBoolean();
            messageLength = tl.nextInt(0, 2001);
            bytesCount = tl.nextLong(0L, CommonConstants.MAX_FILE_SIZE);
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

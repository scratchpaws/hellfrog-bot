package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.core.LogsStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ServerPreferencesDAOTest {

    @Test
    public void testValues() throws Exception {

        Map<Long, ServerSettings> servers = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            while (true) {
                long serverId = TestUtils.randomDiscordEntityId();
                if (!servers.containsKey(serverId)) {
                    ServerSettings sset = new ServerSettings();
                    servers.put(serverId, sset);
                    break;
                }
            }
        }

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            ServerPreferencesDAO preferencesDAO = mainDBController.getServerPreferencesDAO();

            String awaitPrefix = ServerPreferencesDAO.PREFIX_DEFAULT;
            boolean awaitJoinLeaveDisplay = ServerPreferencesDAO.JOIN_LEAVE_DISPLAY_DEFAULT;
            long awaitJoinLeaveChannelId = ServerPreferencesDAO.JOIN_LEAVE_CHANNEL_ID_DEFAULT;
            boolean awaitNewAclMode = ServerPreferencesDAO.NEW_ACL_MODE_DEFAULT;
            boolean awaitCongratulationsEnabled = ServerPreferencesDAO.CONGRATULATIONS_ENABLED_DEFAULT;
            long awaitCongratulationsChannelId = ServerPreferencesDAO.CONGRATULATIONS_CHANNEL_DEFAULT;
            String awaitCongratulationsTimeZoneId = ServerPreferencesDAO.CONGRATULATIONS_TIMEZONE_DEFAULT;
            boolean awaitEnabledStatistic = ServerPreferencesDAO.STATISTIC_ENABLED_DEFAULT;
            Instant awaitStatisticDate = ServerPreferencesDAO.STATISTIC_START_DATE_DEFAULT;

            // извлечение значений по-умолчанию
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();

                String prefix = preferencesDAO.getPrefix(serverId);
                boolean joinLeaveDisplay = preferencesDAO.isJoinLeaveDisplay(serverId);
                long joinLeaveChannel = preferencesDAO.getJoinLeaveChannel(serverId);
                boolean newAclMode = preferencesDAO.isNewAclMode(serverId);
                boolean congratulationsEnabled = preferencesDAO.isCongratulationsEnabled(serverId);
                long congratulationsChannelId = preferencesDAO.getCongratulationChannel(serverId);
                String congratulationsTimeZoneId = preferencesDAO.getCongratulationTimeZone(serverId);
                boolean enabledStatistic = preferencesDAO.isStatisticEnabled(serverId);
                Instant statisticDate = preferencesDAO.getStatisticStartDate(serverId);

                Assertions.assertEquals(awaitPrefix, prefix);
                Assertions.assertEquals(awaitJoinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(awaitJoinLeaveChannelId, joinLeaveChannel);
                Assertions.assertEquals(awaitNewAclMode, newAclMode);
                Assertions.assertEquals(awaitCongratulationsEnabled, congratulationsEnabled);
                Assertions.assertEquals(awaitCongratulationsChannelId, congratulationsChannelId);
                Assertions.assertEquals(awaitCongratulationsTimeZoneId, congratulationsTimeZoneId);
                Assertions.assertEquals(awaitEnabledStatistic, enabledStatistic);
                Assertions.assertEquals(awaitStatisticDate, statisticDate);
            }

            // задание значений, здесь извлекаются всё ещё значения по-умолчанию
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();
                ServerSettings settings = entry.getValue();

                String prefix = preferencesDAO.setPrefix(serverId, settings.prefix);
                boolean joinLeaveDisplay = preferencesDAO.setJoinLeaveDisplay(serverId, settings.joinLeaveDisplay);
                long joinLeaveChannel = preferencesDAO.setJoinLeaveChannel(serverId, settings.joinLeaveChannel);
                boolean newAclMode = preferencesDAO.setNewAclMode(serverId, settings.newAclMode);
                boolean congratulationsEnabled = preferencesDAO.setCongratulationEnabled(serverId, settings.enabledCongratulations);
                long congratulationsChannelId = preferencesDAO.setCongratulationChannel(serverId, settings.congratulationsChannel);
                String congratulationsTimeZoneId = preferencesDAO.setCongratulationTimeZone(serverId, settings.congratulationsTimezone);
                boolean enabledStatistic = preferencesDAO.setStatisticEnabled(serverId, settings.enableStatistic);
                Instant statisticDate = preferencesDAO.setStatisticStartDate(serverId, settings.statisticStartDate);

                Assertions.assertEquals(awaitPrefix, prefix);
                Assertions.assertEquals(awaitJoinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(awaitJoinLeaveChannelId, joinLeaveChannel);
                Assertions.assertEquals(awaitNewAclMode, newAclMode);
                Assertions.assertEquals(awaitCongratulationsEnabled, congratulationsEnabled);
                Assertions.assertEquals(awaitCongratulationsChannelId, congratulationsChannelId);
                Assertions.assertEquals(awaitCongratulationsTimeZoneId, congratulationsTimeZoneId);
                Assertions.assertEquals(awaitEnabledStatistic, enabledStatistic);
                Assertions.assertEquals(awaitStatisticDate, statisticDate);
            }

            // проверяем, что значения сохранились
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();
                ServerSettings settings = entry.getValue();

                String prefix = preferencesDAO.getPrefix(serverId);
                boolean joinLeaveDisplay = preferencesDAO.isJoinLeaveDisplay(serverId);
                long joinLeaveChannel = preferencesDAO.getJoinLeaveChannel(serverId);
                boolean newAclMode = preferencesDAO.isNewAclMode(serverId);
                boolean congratulationsEnabled = preferencesDAO.isCongratulationsEnabled(serverId);
                long congratulationsChannelId = preferencesDAO.getCongratulationChannel(serverId);
                String congratulationsTimeZoneId = preferencesDAO.getCongratulationTimeZone(serverId);
                boolean enabledStatistic = preferencesDAO.isStatisticEnabled(serverId);
                Instant statisticDate = preferencesDAO.getStatisticStartDate(serverId);

                Assertions.assertEquals(settings.prefix, prefix);
                Assertions.assertEquals(settings.joinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(settings.joinLeaveChannel, joinLeaveChannel);
                Assertions.assertEquals(settings.newAclMode, newAclMode);
                Assertions.assertEquals(settings.enabledCongratulations, congratulationsEnabled);
                Assertions.assertEquals(settings.congratulationsChannel, congratulationsChannelId);
                Assertions.assertEquals(settings.congratulationsTimezone, congratulationsTimeZoneId);
                Assertions.assertEquals(settings.enableStatistic, enabledStatistic);
                Assertions.assertEquals(settings.statisticStartDate, statisticDate);
            }

            // проверяем, что значения не перезаписались дефолтом при извлечении данных
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();
                ServerSettings settings = entry.getValue();

                String prefix = preferencesDAO.getPrefix(serverId);
                boolean joinLeaveDisplay = preferencesDAO.isJoinLeaveDisplay(serverId);
                long joinLeaveChannel = preferencesDAO.getJoinLeaveChannel(serverId);
                boolean newAclMode = preferencesDAO.isNewAclMode(serverId);
                boolean congratulationsEnabled = preferencesDAO.isCongratulationsEnabled(serverId);
                long congratulationsChannelId = preferencesDAO.getCongratulationChannel(serverId);
                String congratulationsTimeZoneId = preferencesDAO.getCongratulationTimeZone(serverId);
                boolean enabledStatistic = preferencesDAO.isStatisticEnabled(serverId);
                Instant statisticDate = preferencesDAO.getStatisticStartDate(serverId);

                Assertions.assertEquals(settings.prefix, prefix);
                Assertions.assertEquals(settings.joinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(settings.joinLeaveChannel, joinLeaveChannel);
                Assertions.assertEquals(settings.newAclMode, newAclMode);
                Assertions.assertEquals(settings.enabledCongratulations, congratulationsEnabled);
                Assertions.assertEquals(settings.congratulationsChannel, congratulationsChannelId);
                Assertions.assertEquals(settings.congratulationsTimezone, congratulationsTimeZoneId);
                Assertions.assertEquals(settings.enableStatistic, enabledStatistic);
                Assertions.assertEquals(settings.statisticStartDate, statisticDate);
            }
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            ServerPreferencesDAO preferencesDAO = mainDBController.getServerPreferencesDAO();

            // проверяем, что значения сохранились после закрытия БД
            for (Map.Entry<Long, ServerSettings> entry : servers.entrySet()) {
                long serverId = entry.getKey();
                ServerSettings settings = entry.getValue();

                String prefix = preferencesDAO.getPrefix(serverId);
                boolean joinLeaveDisplay = preferencesDAO.isJoinLeaveDisplay(serverId);
                long joinLeaveChannel = preferencesDAO.getJoinLeaveChannel(serverId);
                boolean newAclMode = preferencesDAO.isNewAclMode(serverId);
                boolean congratulationsEnabled = preferencesDAO.isCongratulationsEnabled(serverId);
                long congratulationsChannelId = preferencesDAO.getCongratulationChannel(serverId);
                String congratulationsTimeZoneId = preferencesDAO.getCongratulationTimeZone(serverId);
                boolean enabledStatistic = preferencesDAO.isStatisticEnabled(serverId);
                Instant statisticDate = preferencesDAO.getStatisticStartDate(serverId);

                Assertions.assertEquals(settings.prefix, prefix);
                Assertions.assertEquals(settings.joinLeaveDisplay, joinLeaveDisplay);
                Assertions.assertEquals(settings.joinLeaveChannel, joinLeaveChannel);
                Assertions.assertEquals(settings.newAclMode, newAclMode);
                Assertions.assertEquals(settings.enabledCongratulations, congratulationsEnabled);
                Assertions.assertEquals(settings.congratulationsChannel, congratulationsChannelId);
                Assertions.assertEquals(settings.congratulationsTimezone, congratulationsTimeZoneId);
                Assertions.assertEquals(settings.enableStatistic, enabledStatistic);
                Assertions.assertEquals(settings.statisticStartDate, statisticDate);
            }
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }

    private static class ServerSettings {

        final String prefix;
        final boolean joinLeaveDisplay;
        final long joinLeaveChannel;
        final boolean newAclMode;
        final boolean enabledCongratulations;
        final long congratulationsChannel;
        final String congratulationsTimezone;
        final boolean enableStatistic;
        final Instant statisticStartDate;

        ServerSettings() {
            ThreadLocalRandom lr = ThreadLocalRandom.current();
            prefix = TestUtils.randomStringName(3);
            joinLeaveDisplay = lr.nextBoolean();
            joinLeaveChannel = TestUtils.randomDiscordEntityId();
            newAclMode = lr.nextBoolean();
            enabledCongratulations = lr.nextBoolean();
            congratulationsChannel = TestUtils.randomDiscordEntityId();
            congratulationsTimezone = TestUtils.randomTimeZone().getID();
            enableStatistic = lr.nextBoolean();
            statisticStartDate = TestUtils.randomInstant();
        }
    }
}

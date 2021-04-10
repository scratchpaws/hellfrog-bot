package hellfrog.settings.db;

import hellfrog.TestUtils;
import hellfrog.core.LogsStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommonPreferencesDAOTest {

    @Test
    public void testSetValues() throws Exception {

        String botName = TestUtils.randomStringName(5);
        String botPrefix = TestUtils.randomStringName(2);
        String botApi = TestUtils.randomStringName(64);
        long officialServerId = TestUtils.randomDiscordEntityId();
        long serviceChannelId = TestUtils.randomDiscordEntityId();

        long highChannelId = TestUtils.randomDiscordEntityId();
        long lowChannelId = TestUtils.randomDiscordEntityId();

        long funBlushChannelId = TestUtils.randomDiscordEntityId();
        long funHugChannelId = TestUtils.randomDiscordEntityId();
        long funKissChannelId = TestUtils.randomDiscordEntityId();
        long funLoveChannelId = TestUtils.randomDiscordEntityId();
        long funPatChannelId = TestUtils.randomDiscordEntityId();
        long funShockChannelId = TestUtils.randomDiscordEntityId();
        long funSlapChannelId = TestUtils.randomDiscordEntityId();
        long funCuddleChannelId = TestUtils.randomDiscordEntityId();
        long funDanceChannelId = TestUtils.randomDiscordEntityId();
        long funLickChannelId = TestUtils.randomDiscordEntityId();
        long funBiteChannelId = TestUtils.randomDiscordEntityId();

        MainDBController.destroyTestDatabase();
        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            CommonPreferencesDAO preferencesDAO = mainDBController.getCommonPreferencesDAO();

            // проверяем извлечение умолчаний
            String oldKey = preferencesDAO.getApiKey();
            String oldName = preferencesDAO.getBotName();
            String oldPrefix = preferencesDAO.getBotPrefix();
            long oldOfficialServerId = preferencesDAO.getOfficialBotServerId();
            long oldServiceChannelId = preferencesDAO.getBotServiceChannelId();

            long oldHighChannelId = preferencesDAO.getHighRollChannelId();
            long oldLowChannelId = preferencesDAO.getLowRollChannelId();

            long oldFunBlushChannelId = preferencesDAO.getFunBlushChannel();
            long oldFunHugChannelId = preferencesDAO.getFunHugChannel();
            long oldFunKissChannelId = preferencesDAO.getFunKissChannel();
            long oldFunLoveChannelId = preferencesDAO.getFunLoveChannel();
            long oldFunPatChannelId = preferencesDAO.getFunPatChannel();
            long oldFunShockChannelId = preferencesDAO.getFunShockChannel();
            long oldFunSlapChannelId = preferencesDAO.getFunSlapChannel();
            long oldFunCuddleChannelId = preferencesDAO.getFunCuddleChannel();
            long oldFunDanceChannelId = preferencesDAO.getFunDanceChannel();
            long oldFunLickChannelId = preferencesDAO.getFunLickChannel();
            long oldFunBiteChannelId = preferencesDAO.getFunBiteChannel();

            Assertions.assertEquals(CommonPreferencesDAO.API_KEY_DEFAULT, oldKey);
            Assertions.assertEquals(CommonPreferencesDAO.BOT_NAME_DEFAULT, oldName);
            Assertions.assertEquals(CommonPreferencesDAO.PREFIX_DEFAULT, oldPrefix);
            Assertions.assertEquals(CommonPreferencesDAO.OFFICIAL_SERVER_DEFAULT, oldOfficialServerId);
            Assertions.assertEquals(CommonPreferencesDAO.SERVICE_CHANNEL_DEFAULT, oldServiceChannelId);

            Assertions.assertEquals(CommonPreferencesDAO.HIGH_ROLL_CHANNEL_KEY_DEFAULT, oldHighChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.LOW_ROLL_CHANNEL_DEFAULT, oldLowChannelId);

            Assertions.assertEquals(CommonPreferencesDAO.FUN_BLUSH_CHANNEL_DEFAULT, oldFunBlushChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_HUG_CHANNEL_DEFAULT, oldFunHugChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_KISS_CHANNEL_DEFAULT, oldFunKissChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_LOVE_CHANNEL_DEFAULT, oldFunLoveChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_PAT_CHANNEL_DEFAULT, oldFunPatChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_SHOCK_CHANNEL_DEFAULT, oldFunShockChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_SLAP_CHANNEL_DEFAULT, oldFunSlapChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_CUDDLE_CHANNEL_DEFAULT, oldFunCuddleChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_DANCE_CHANNEL_DEFAULT, oldFunDanceChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_LICK_CHANNEL_DEFAULT, oldFunLickChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_BITE_CHANNEL_DEFAULT, oldFunBiteChannelId);

            // проверяем замену данных, должны извлечься умолчания
            oldKey = preferencesDAO.setApiKey(botApi);
            oldName = preferencesDAO.setBotName(botName);
            oldPrefix = preferencesDAO.setBotPrefix(botPrefix);
            oldOfficialServerId = preferencesDAO.setOfficialBotServerId(officialServerId);
            oldServiceChannelId = preferencesDAO.setBotServiceChannelId(serviceChannelId);

            oldHighChannelId = preferencesDAO.setHighRollChannelId(highChannelId);
            oldLowChannelId = preferencesDAO.setLowRollChannelId(lowChannelId);

            oldFunBlushChannelId = preferencesDAO.setFunBlushChannel(funBlushChannelId);
            oldFunHugChannelId = preferencesDAO.setFunHugChannel(funHugChannelId);
            oldFunKissChannelId = preferencesDAO.setFunKissChannel(funKissChannelId);
            oldFunLoveChannelId = preferencesDAO.setFunLoveChannel(funLoveChannelId);
            oldFunPatChannelId = preferencesDAO.setFunPatChannel(funPatChannelId);
            oldFunShockChannelId = preferencesDAO.setFunShockChannel(funShockChannelId);
            oldFunSlapChannelId = preferencesDAO.setFunSlapChannel(funSlapChannelId);
            oldFunCuddleChannelId = preferencesDAO.setFunCuddleChannel(funCuddleChannelId);
            oldFunDanceChannelId = preferencesDAO.setFunDanceChannel(funDanceChannelId);
            oldFunLickChannelId = preferencesDAO.setFunLickChannel(funLickChannelId);
            oldFunBiteChannelId = preferencesDAO.setFunBiteChannel(funBiteChannelId);

            Assertions.assertEquals(CommonPreferencesDAO.API_KEY_DEFAULT, oldKey);
            Assertions.assertEquals(CommonPreferencesDAO.BOT_NAME_DEFAULT, oldName);
            Assertions.assertEquals(CommonPreferencesDAO.PREFIX_DEFAULT, oldPrefix);
            Assertions.assertEquals(CommonPreferencesDAO.OFFICIAL_SERVER_DEFAULT, oldOfficialServerId);
            Assertions.assertEquals(CommonPreferencesDAO.SERVICE_CHANNEL_DEFAULT, oldServiceChannelId);

            Assertions.assertEquals(CommonPreferencesDAO.HIGH_ROLL_CHANNEL_KEY_DEFAULT, oldHighChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.LOW_ROLL_CHANNEL_DEFAULT, oldLowChannelId);

            Assertions.assertEquals(CommonPreferencesDAO.FUN_BLUSH_CHANNEL_DEFAULT, oldFunBlushChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_HUG_CHANNEL_DEFAULT, oldFunHugChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_KISS_CHANNEL_DEFAULT, oldFunKissChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_LOVE_CHANNEL_DEFAULT, oldFunLoveChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_PAT_CHANNEL_DEFAULT, oldFunPatChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_SHOCK_CHANNEL_DEFAULT, oldFunShockChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_SLAP_CHANNEL_DEFAULT, oldFunSlapChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_CUDDLE_CHANNEL_DEFAULT, oldFunCuddleChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_DANCE_CHANNEL_DEFAULT, oldFunDanceChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_LICK_CHANNEL_DEFAULT, oldFunLickChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.FUN_BITE_CHANNEL_DEFAULT, oldFunBiteChannelId);

            // проверяем, что данные сохранились
            String newKey = preferencesDAO.getApiKey();
            String newName = preferencesDAO.getBotName();
            String newPrefix = preferencesDAO.getBotPrefix();
            long newOfficialServerId = preferencesDAO.getOfficialBotServerId();
            long newServiceChannelId = preferencesDAO.getBotServiceChannelId();

            long newHighChannelId = preferencesDAO.getHighRollChannelId();
            long newLowChannelId = preferencesDAO.getLowRollChannelId();

            long newFunBlushChannelId = preferencesDAO.getFunBlushChannel();
            long newFunHugChannelId = preferencesDAO.getFunHugChannel();
            long newFunKissChannelId = preferencesDAO.getFunKissChannel();
            long newFunLoveChannelId = preferencesDAO.getFunLoveChannel();
            long newFunPatChannelId = preferencesDAO.getFunPatChannel();
            long newFunShockChannelId = preferencesDAO.getFunShockChannel();
            long newFunSlapChannelId = preferencesDAO.getFunSlapChannel();
            long newFunCuddleChannelId = preferencesDAO.getFunCuddleChannel();
            long newFunDanceChannelId = preferencesDAO.getFunDanceChannel();
            long newFunLickChannelId = preferencesDAO.getFunLickChannel();
            long newFunBiteChannelId = preferencesDAO.getFunBiteChannel();

            Assertions.assertEquals(botPrefix, newPrefix);
            Assertions.assertEquals(botName, newName);
            Assertions.assertEquals(botApi, newKey);
            Assertions.assertEquals(officialServerId, newOfficialServerId);
            Assertions.assertEquals(serviceChannelId, newServiceChannelId);

            Assertions.assertEquals(highChannelId, newHighChannelId);
            Assertions.assertEquals(lowChannelId, newLowChannelId);

            Assertions.assertEquals(funBlushChannelId, newFunBlushChannelId);
            Assertions.assertEquals(funHugChannelId, newFunHugChannelId);
            Assertions.assertEquals(funKissChannelId, newFunKissChannelId);
            Assertions.assertEquals(funLoveChannelId, newFunLoveChannelId);
            Assertions.assertEquals(funPatChannelId, newFunPatChannelId);
            Assertions.assertEquals(funShockChannelId, newFunShockChannelId);
            Assertions.assertEquals(funSlapChannelId, newFunSlapChannelId);
            Assertions.assertEquals(funCuddleChannelId, newFunCuddleChannelId);
            Assertions.assertEquals(funDanceChannelId, newFunDanceChannelId);
            Assertions.assertEquals(funLickChannelId, newFunLickChannelId);
            Assertions.assertEquals(funBiteChannelId, newFunBiteChannelId);

            // проверяем, что данные сохранились при первичном чтении (not override)
            newKey = preferencesDAO.getApiKey();
            newName = preferencesDAO.getBotName();
            newPrefix = preferencesDAO.getBotPrefix();
            newOfficialServerId = preferencesDAO.getOfficialBotServerId();
            newServiceChannelId = preferencesDAO.getBotServiceChannelId();

            newHighChannelId = preferencesDAO.getHighRollChannelId();
            newLowChannelId = preferencesDAO.getLowRollChannelId();

            newFunBlushChannelId = preferencesDAO.getFunBlushChannel();
            newFunHugChannelId = preferencesDAO.getFunHugChannel();
            newFunKissChannelId = preferencesDAO.getFunKissChannel();
            newFunLoveChannelId = preferencesDAO.getFunLoveChannel();
            newFunPatChannelId = preferencesDAO.getFunPatChannel();
            newFunShockChannelId = preferencesDAO.getFunShockChannel();
            newFunSlapChannelId = preferencesDAO.getFunSlapChannel();
            newFunCuddleChannelId = preferencesDAO.getFunCuddleChannel();
            newFunDanceChannelId = preferencesDAO.getFunDanceChannel();
            newFunLickChannelId = preferencesDAO.getFunLickChannel();
            newFunBiteChannelId = preferencesDAO.getFunBiteChannel();

            Assertions.assertEquals(botPrefix, newPrefix);
            Assertions.assertEquals(botName, newName);
            Assertions.assertEquals(botApi, newKey);
            Assertions.assertEquals(officialServerId, newOfficialServerId);
            Assertions.assertEquals(serviceChannelId, newServiceChannelId);

            Assertions.assertEquals(highChannelId, newHighChannelId);
            Assertions.assertEquals(lowChannelId, newLowChannelId);

            Assertions.assertEquals(funBlushChannelId, newFunBlushChannelId);
            Assertions.assertEquals(funHugChannelId, newFunHugChannelId);
            Assertions.assertEquals(funKissChannelId, newFunKissChannelId);
            Assertions.assertEquals(funLoveChannelId, newFunLoveChannelId);
            Assertions.assertEquals(funPatChannelId, newFunPatChannelId);
            Assertions.assertEquals(funShockChannelId, newFunShockChannelId);
            Assertions.assertEquals(funSlapChannelId, newFunSlapChannelId);
            Assertions.assertEquals(funCuddleChannelId, newFunCuddleChannelId);
            Assertions.assertEquals(funDanceChannelId, newFunDanceChannelId);
            Assertions.assertEquals(funLickChannelId, newFunLickChannelId);
            Assertions.assertEquals(funBiteChannelId, newFunBiteChannelId);
        }

        try (MainDBController mainDBController = MainDBController.getInstance(InstanceType.TEST)) {
            CommonPreferencesDAO preferencesDAO = mainDBController.getCommonPreferencesDAO();
            // повторная проверка с новым переподключением
            String newKey = preferencesDAO.getApiKey();
            String newName = preferencesDAO.getBotName();
            String newPrefix = preferencesDAO.getBotPrefix();
            long newOfficialServerId = preferencesDAO.getOfficialBotServerId();
            long newServiceChannelId = preferencesDAO.getBotServiceChannelId();

            long newHighChannelId = preferencesDAO.getHighRollChannelId();
            long newLowChannelId = preferencesDAO.getLowRollChannelId();

            Assertions.assertEquals(botPrefix, newPrefix);
            Assertions.assertEquals(botName, newName);
            Assertions.assertEquals(botApi, newKey);
            Assertions.assertEquals(officialServerId, newOfficialServerId);
            Assertions.assertEquals(serviceChannelId, newServiceChannelId);

            Assertions.assertEquals(highChannelId, newHighChannelId);
            Assertions.assertEquals(lowChannelId, newLowChannelId);

            long newFunBlushChannelId = preferencesDAO.getFunBlushChannel();
            long newFunHugChannelId = preferencesDAO.getFunHugChannel();
            long newFunKissChannelId = preferencesDAO.getFunKissChannel();
            long newFunLoveChannelId = preferencesDAO.getFunLoveChannel();
            long newFunPatChannelId = preferencesDAO.getFunPatChannel();
            long newFunShockChannelId = preferencesDAO.getFunShockChannel();
            long newFunSlapChannelId = preferencesDAO.getFunSlapChannel();
            long newFunCuddleChannelId = preferencesDAO.getFunCuddleChannel();
            long newFunDanceChannelId = preferencesDAO.getFunDanceChannel();
            long newFunLickChannelId = preferencesDAO.getFunLickChannel();
            long newFunBiteChannelId = preferencesDAO.getFunBiteChannel();

            Assertions.assertEquals(funBlushChannelId, newFunBlushChannelId);
            Assertions.assertEquals(funHugChannelId, newFunHugChannelId);
            Assertions.assertEquals(funKissChannelId, newFunKissChannelId);
            Assertions.assertEquals(funLoveChannelId, newFunLoveChannelId);
            Assertions.assertEquals(funPatChannelId, newFunPatChannelId);
            Assertions.assertEquals(funShockChannelId, newFunShockChannelId);
            Assertions.assertEquals(funSlapChannelId, newFunSlapChannelId);
            Assertions.assertEquals(funCuddleChannelId, newFunCuddleChannelId);
            Assertions.assertEquals(funDanceChannelId, newFunDanceChannelId);
            Assertions.assertEquals(funLickChannelId, newFunLickChannelId);
            Assertions.assertEquals(funBiteChannelId, newFunBiteChannelId);
        }

        Assertions.assertTrue(LogsStorage.isErrorsEmpty(), "Errors log must be empty");
        Assertions.assertTrue(LogsStorage.isWarnsEmpty(), "Warning logs must be empty");
    }
}

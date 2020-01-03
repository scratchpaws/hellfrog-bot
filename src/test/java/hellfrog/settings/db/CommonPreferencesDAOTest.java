package hellfrog.settings.db;

import hellfrog.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonPreferencesDAOTest {

    private static final Path SETTINGS_PATH = Paths.get("./settings/");
    private static final String TEST_DB_NAME = "test.sqlite3";
    private static final Path tstBase = SETTINGS_PATH.resolve(TEST_DB_NAME);

    @Test
    public void testSetValues() throws Exception {

        String botName = TestUtils.randomStringName(5);
        String botPrefix = TestUtils.randomStringName(2);
        String botApi = TestUtils.randomStringName(64);
        long officialServerId = TestUtils.randomDiscordEntityId();
        long serviceChannelId = TestUtils.randomDiscordEntityId();
        long highChannelId = TestUtils.randomDiscordEntityId();
        long lowChannelId = TestUtils.randomDiscordEntityId();

        Files.deleteIfExists(tstBase);
        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            CommonPreferencesDAO preferencesDAO = mainDBController.getCommonPreferencesDAO();

            // проверяем извлечение умолчаний
            String oldKey = preferencesDAO.getApiKey();
            String oldName = preferencesDAO.getBotName();
            String oldPrefix = preferencesDAO.getBotPrefix();
            long oldOfficialServerId = preferencesDAO.getOfficialBotServerId();
            long oldServiceChannelId = preferencesDAO.getBotServiceChannelId();
            long oldHighChannelId = preferencesDAO.getHighRollChannelId();
            long oldLowChannelId = preferencesDAO.getLowRollChannelId();

            Assertions.assertEquals(CommonPreferencesDAO.API_KEY_DEFAULT, oldKey);
            Assertions.assertEquals(CommonPreferencesDAO.BOT_NAME_DEFAULT, oldName);
            Assertions.assertEquals(CommonPreferencesDAO.PREFIX_DEFAULT, oldPrefix);
            Assertions.assertEquals(CommonPreferencesDAO.OFFICIAL_SERVER_DEFAULT, oldOfficialServerId);
            Assertions.assertEquals(CommonPreferencesDAO.SERVICE_CHANNEL_DEFAULT, oldServiceChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.HIGH_ROLL_CHANNEL_KEY_DEFAULT, oldHighChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.LOW_ROLL_CHANNEL_DEFAULT, oldLowChannelId);

            // проверяем замену данных, должны извлечься умолчания
            oldKey = preferencesDAO.setApiKey(botApi);
            oldName = preferencesDAO.setBotName(botName);
            oldPrefix = preferencesDAO.setBotPrefix(botPrefix);
            oldOfficialServerId = preferencesDAO.setOfficialBotServerId(officialServerId);
            oldServiceChannelId = preferencesDAO.setBotServiceChannelId(serviceChannelId);
            oldHighChannelId = preferencesDAO.setHighRollChannelId(highChannelId);
            oldLowChannelId = preferencesDAO.setLowRollChannelId(lowChannelId);

            Assertions.assertEquals(CommonPreferencesDAO.API_KEY_DEFAULT, oldKey);
            Assertions.assertEquals(CommonPreferencesDAO.BOT_NAME_DEFAULT, oldName);
            Assertions.assertEquals(CommonPreferencesDAO.PREFIX_DEFAULT, oldPrefix);
            Assertions.assertEquals(CommonPreferencesDAO.OFFICIAL_SERVER_DEFAULT, oldOfficialServerId);
            Assertions.assertEquals(CommonPreferencesDAO.SERVICE_CHANNEL_DEFAULT, oldServiceChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.HIGH_ROLL_CHANNEL_KEY_DEFAULT, oldHighChannelId);
            Assertions.assertEquals(CommonPreferencesDAO.LOW_ROLL_CHANNEL_DEFAULT, oldLowChannelId);

            // проверяем, что данные сохранились
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

            // проверяем, что данные сохранились при первичном чтении (not override)
            newKey = preferencesDAO.getApiKey();
            newName = preferencesDAO.getBotName();
            newPrefix = preferencesDAO.getBotPrefix();
            newOfficialServerId = preferencesDAO.getOfficialBotServerId();
            newServiceChannelId = preferencesDAO.getBotServiceChannelId();
            newHighChannelId = preferencesDAO.getHighRollChannelId();
            newLowChannelId = preferencesDAO.getLowRollChannelId();
            Assertions.assertEquals(botPrefix, newPrefix);
            Assertions.assertEquals(botName, newName);
            Assertions.assertEquals(botApi, newKey);
            Assertions.assertEquals(officialServerId, newOfficialServerId);
            Assertions.assertEquals(serviceChannelId, newServiceChannelId);
            Assertions.assertEquals(highChannelId, newHighChannelId);
            Assertions.assertEquals(lowChannelId, newLowChannelId);
        }

        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
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
        }
    }
}

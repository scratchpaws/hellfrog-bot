package hellfrog.settings.db;

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

        String botName = "Bruva";
        String botPrefix = "!>";
        String botApi = "API_KEY";

        Files.deleteIfExists(tstBase);
        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            CommonPreferencesDAO preferencesDAO = mainDBController.getCommonPreferencesDAO();

            // проверяем извлечение умолчаний
            String oldKey = preferencesDAO.getApiKey();
            String oldName = preferencesDAO.getBotName();
            String oldPrefix = preferencesDAO.getBotPrefix();

            Assertions.assertEquals(CommonPreferencesDAO.API_KEY_DEFAULT, oldKey);
            Assertions.assertEquals(CommonPreferencesDAO.BOT_NAME_DEFAULT, oldName);
            Assertions.assertEquals(CommonPreferencesDAO.PREFIX_DEFAULT, oldPrefix);

            // проверяем замену данных, должны извлечься умолчания
            oldKey = preferencesDAO.setApiKey(botApi);
            oldName = preferencesDAO.setBotName(botName);
            oldPrefix = preferencesDAO.setBotPrefix(botPrefix);

            Assertions.assertEquals(CommonPreferencesDAO.API_KEY_DEFAULT, oldKey);
            Assertions.assertEquals(CommonPreferencesDAO.BOT_NAME_DEFAULT, oldName);
            Assertions.assertEquals(CommonPreferencesDAO.PREFIX_DEFAULT, oldPrefix);

            // проверяем, что данные сохранились
            String newKey = preferencesDAO.getApiKey();
            String newName = preferencesDAO.getBotName();
            String newPrefix = preferencesDAO.getBotPrefix();
            Assertions.assertEquals(botPrefix, newPrefix);
            Assertions.assertEquals(botName, newName);
            Assertions.assertEquals(botApi, newKey);

            // проверяем, что данные сохранились при первичном чтении (not override)
            newKey = preferencesDAO.getApiKey();
            newName = preferencesDAO.getBotName();
            newPrefix = preferencesDAO.getBotPrefix();
            Assertions.assertEquals(botPrefix, newPrefix);
            Assertions.assertEquals(botName, newName);
            Assertions.assertEquals(botApi, newKey);
        }

        try (MainDBController mainDBController = new MainDBController(TEST_DB_NAME, false)) {
            CommonPreferencesDAO preferencesDAO = mainDBController.getCommonPreferencesDAO();
            // повторная проверка с новым переподключением
            String newKey = preferencesDAO.getApiKey();
            String newName = preferencesDAO.getBotName();
            String newPrefix = preferencesDAO.getBotPrefix();
            Assertions.assertEquals(botPrefix, newPrefix);
            Assertions.assertEquals(botName, newName);
            Assertions.assertEquals(botApi, newKey);
        }
    }
}

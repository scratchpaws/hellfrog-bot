package bruva.settings;

import bruva.core.HibernateUtils;
import bruva.settings.Entity.CommonName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class SettingsControllerTest {

    @BeforeAll
    public static void openTest() throws Exception {
        Files.deleteIfExists(Paths.get("./settings/db_test.mv.db"));
        HibernateUtils.setHibernateRegistryName("hibernate_test.cfg.xml");
        HibernateUtils.getSessionFactory();
    }

    @Test
    public void defaultBotPrefixText() throws Exception {
        SettingsController settingsController = SettingsController.getInstance();
        String await = CommonName.DEFAULT_VALUES.get(CommonName.BOT_PREFIX);
        Optional<String> mayBeBotName = settingsController.getBotPrefix();
        Assertions.assertTrue(mayBeBotName.isPresent());
        String value = mayBeBotName.orElse(null);
        Assertions.assertEquals(await, value);

        await = "x>";
    }

    @AfterAll
    public static void closeText() throws Exception {
        HibernateUtils.close();

    }
}

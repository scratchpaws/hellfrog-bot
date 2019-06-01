package bruva.settings;

import bruva.core.HibernateUtils;
import bruva.settings.Entity.CommonName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.Table;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class SettingsControllerTest {

    @BeforeAll
    public static void openTest() throws Exception {
        Files.deleteIfExists(Paths.get("./settings/db_test.mv.db"));
        HibernateUtils.setHibernateRegistryName("hibernate_test.cfg.xml");
        HibernateUtils.getSessionFactory();
    }

    @Test
    public void botPrefixText() {
        SettingsController settingsController = SettingsController.getInstance();
        String await = CommonName.DEFAULT_VALUES.get(CommonName.BOT_PREFIX);
        Optional<String> mayBeBotPrefix = settingsController.getBotPrefix();
        Assertions.assertTrue(mayBeBotPrefix.isPresent());
        String value = mayBeBotPrefix.orElse(null);
        Assertions.assertEquals(await, value);

        await = "x>";
        boolean successChanges = settingsController.setBotPrefix(await);
        Assertions.assertTrue(successChanges);
        mayBeBotPrefix = settingsController.getBotPrefix();
        Assertions.assertTrue(mayBeBotPrefix.isPresent());
        value = mayBeBotPrefix.orElse(null);
        Assertions.assertEquals(await, value);
    }

    @Test
    public void botNameTest() {
        SettingsController settingsController = SettingsController.getInstance();
        String await = CommonName.DEFAULT_VALUES.get(CommonName.BOT_NAME);
        Optional<String> mayBeBotName = settingsController.getBotName();
        Assertions.assertTrue(mayBeBotName.isPresent());
        String value = mayBeBotName.orElse(null);
        Assertions.assertEquals(await, value);

        await = "Bruva";
        boolean successChanges = settingsController.setBotName(await);
        Assertions.assertTrue(successChanges);
        mayBeBotName = settingsController.getBotName();
        Assertions.assertTrue(mayBeBotName.isPresent());
        value = mayBeBotName.orElse(null);
        Assertions.assertEquals(await, value);
    }

    @Test
    public void remoteDebugTest() {
        SettingsController settingsController = SettingsController.getInstance();
        String raw = CommonName.DEFAULT_VALUES.get(CommonName.BOT_NAME);
        boolean await = Boolean.parseBoolean(raw);
        Optional<Boolean> mayBeRemoteState = settingsController.isRemoteDebugEnabled();
        Assertions.assertTrue(mayBeRemoteState.isPresent());
        boolean value = mayBeRemoteState.get();
        Assertions.assertEquals(await, value);

        await = !await;
        boolean successChanges = settingsController.setRemoteDebugEnable(await);
        Assertions.assertTrue(successChanges);
        mayBeRemoteState = settingsController.isRemoteDebugEnabled();
        Assertions.assertTrue(mayBeRemoteState.isPresent());
        value = mayBeRemoteState.get();
        Assertions.assertEquals(await, value);
    }

    @Test
    public void botOwnerTest() {
        SettingsController settingsController = SettingsController.getInstance();
        Optional<List<Long>> mayBeList = settingsController.getAllBotOwners();
        Assertions.assertTrue(mayBeList.isPresent());
        Assertions.assertTrue(mayBeList.get().isEmpty());

        long first = 1234567890L;
        long second = 9876543210L;
        boolean result = settingsController.addBotOwner(first);
        Assertions.assertTrue(result);
        result = settingsController.addBotOwner(second);
        Assertions.assertTrue(result);
        result = settingsController.addBotOwner(first);
        Assertions.assertFalse(result);
        mayBeList = settingsController.getAllBotOwners();
        Assertions.assertTrue(mayBeList.isPresent());
        Assertions.assertFalse(mayBeList.get().isEmpty());
        List<Long> botOwners = mayBeList.get();
        Assertions.assertTrue(botOwners.contains(first) && botOwners.contains(second));

        result = settingsController.deleteBotOwner(first);
        Assertions.assertTrue(result);
        result = settingsController.deleteBotOwner(first);
        Assertions.assertFalse(result);
        mayBeList = settingsController.getAllBotOwners();
        Assertions.assertTrue(mayBeList.isPresent());
        Assertions.assertFalse(mayBeList.get().isEmpty());
        botOwners = mayBeList.get();
        Assertions.assertTrue(botOwners.contains(second) && !botOwners.contains(first));
    }

    @AfterAll
    public static void closeText() {
        HibernateUtils.close();

    }
}

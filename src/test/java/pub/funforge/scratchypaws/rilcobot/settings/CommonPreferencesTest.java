package pub.funforge.scratchypaws.rilcobot.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pub.funforge.scratchypaws.rilcobot.settings.old.CommonPreferences;

import java.util.concurrent.CopyOnWriteArrayList;

public class CommonPreferencesTest {

    @Test
    public void testSameObjectClassAndValues() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        CommonPreferences commonPreferences = new CommonPreferences();
        commonPreferences.setBotName("TestBot");
        commonPreferences.setCommonBotPrefix(">>");
        commonPreferences.addGlobalBotOwner(6L);
        commonPreferences.addGlobalBotOwner(7L);
        commonPreferences.setApiKey("some");

        String saved = objectMapper.writeValueAsString(commonPreferences);
        CommonPreferences restored = objectMapper.readValue(saved, CommonPreferences.class);
        Assertions.assertEquals(restored.getGlobalBotOwners().getClass(), CopyOnWriteArrayList.class);
        Assertions.assertTrue(restored.getGlobalBotOwners().contains(6L));
        Assertions.assertTrue(restored.getGlobalBotOwners().contains(7L));
        Assertions.assertEquals(restored.getBotName(), "TestBot");
        Assertions.assertEquals(restored.getCommonBotPrefix(), ">>");
        Assertions.assertEquals(restored.getApiKey(), "some");
    }
}

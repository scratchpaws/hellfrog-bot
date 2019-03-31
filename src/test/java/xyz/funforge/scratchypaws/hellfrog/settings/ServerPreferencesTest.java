package xyz.funforge.scratchypaws.hellfrog.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import xyz.funforge.scratchypaws.hellfrog.settings.old.CommandRights;
import xyz.funforge.scratchypaws.hellfrog.settings.old.ServerPreferences;

public class ServerPreferencesTest {

    @Test
    public void testSameClassesAndValues() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        ServerPreferences serverPreferences = new ServerPreferences();
        serverPreferences.setBotPrefix(">>");
        CommandRights commandRights = serverPreferences.getRightsForCommand("pref");
        Assertions.assertTrue(commandRights.addAllowRole(5L));
        Assertions.assertTrue(commandRights.addAllowUser(6L));
        Assertions.assertTrue(commandRights.addAllowChannel(7L));
        Assertions.assertFalse(commandRights.isAllowUser(1L));
        Assertions.assertFalse(commandRights.isAllowRole(1L));
        Assertions.assertFalse(commandRights.isAllowChat(1L));

        String serialized = objectMapper.writeValueAsString(serverPreferences);
        ServerPreferences restored = objectMapper.readValue(serialized, ServerPreferences.class);
        Assertions.assertEquals(">>", restored.getBotPrefix());
        CommandRights restoredPrefRights = restored.getRightsForCommand("pref");
        Assertions.assertTrue(restoredPrefRights.isAllowRole(5L));
        Assertions.assertTrue(restoredPrefRights.isAllowUser(6L));
        Assertions.assertTrue(restoredPrefRights.isAllowChat(7L));
        Assertions.assertEquals(1, restoredPrefRights.getAllowUsers().size());
        Assertions.assertEquals(1, restoredPrefRights.getAllowRoles().size());
        Assertions.assertEquals(1, restoredPrefRights.getAllowChannels().size());

        CommandRights newbie = restored.getRightsForCommand("help");
        Assertions.assertEquals(0, newbie.getAllowRoles().size());
        Assertions.assertEquals(0, newbie.getAllowUsers().size());
        Assertions.assertEquals(0, newbie.getAllowChannels().size());
    }
}

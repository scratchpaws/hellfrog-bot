package xyz.funforge.scratchypaws.hellfrog.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import xyz.funforge.scratchypaws.hellfrog.settings.old.CommandRights;

import java.util.concurrent.CopyOnWriteArrayList;

public class CommandRightsTest {

    @Test
    public void testSameClassesAndValues() throws Exception {
        CommandRights commandRights = new CommandRights();
        commandRights.setCommandPrefix("pref");
        Assertions.assertTrue(commandRights.addAllowRole(6L));
        Assertions.assertTrue(commandRights.addAllowUser(5L));
        Assertions.assertTrue(commandRights.addAllowChannel(7L));

        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writeValueAsString(commandRights);

        CommandRights restored = objectMapper.readValue(serialized, CommandRights.class);
        Assertions.assertEquals(CopyOnWriteArrayList.class, restored.getAllowRoles().getClass());
        Assertions.assertEquals(CopyOnWriteArrayList.class, restored.getAllowUsers().getClass());
        Assertions.assertEquals(CopyOnWriteArrayList.class, restored.getAllowChannels().getClass());
        Assertions.assertEquals(commandRights.getCommandPrefix(), restored.getCommandPrefix());
        Assertions.assertTrue(restored.isAllowRole(6L));
        Assertions.assertTrue(restored.isAllowUser(5L));
        Assertions.assertTrue(restored.isAllowChat(7L));

        Assertions.assertTrue(commandRights.delAllowRole(6L));
        Assertions.assertTrue(commandRights.delAllowUser(5L));
        Assertions.assertTrue(commandRights.delAllowChannel(7L));
        Assertions.assertFalse(commandRights.isAllowUser(5L));
        Assertions.assertFalse(commandRights.isAllowRole(6L));
        Assertions.assertFalse(commandRights.isAllowChat(7L));
    }
}

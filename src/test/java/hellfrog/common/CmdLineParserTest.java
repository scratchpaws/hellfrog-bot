package hellfrog.common;

import hellfrog.core.EventsListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class CmdLineParserTest {

    @Test
    public void testPrefixCut() throws Exception {
        Path testDir = Paths.get("./settings");
        Path testConfig = testDir.resolve("common.json");
        if (Files.notExists(testDir))
            Files.createDirectories(testDir);
        if (Files.notExists(testConfig)) {
            try (BufferedWriter bufIO = Files.newBufferedWriter(testConfig, StandardCharsets.UTF_8)) {
                bufIO.append("{ \"apiKey\" : \"empty\" }");
            }
        }
        String cmdlineWithSpace = ">> help";
        String cmdlineWithoutSpace = ">>help";
        String prefix = ">>";
        String await = "help";

        EventsListener eventsListener = new EventsListener();

        Assertions.assertEquals(await, eventsListener.getCmdlineWithoutPrefix(prefix, cmdlineWithoutSpace));
        Assertions.assertEquals(await, eventsListener.getCmdlineWithoutPrefix(prefix, cmdlineWithSpace));
    }
}

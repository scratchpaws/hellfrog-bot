package xyz.funforge.scratchypaws.hellfrog.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import xyz.funforge.scratchypaws.hellfrog.core.EventsListener;


public class CmdLineParserTest {

    @Test
    public void testPrefixCut() throws Exception {
        String cmdlineWithSpace = ">> help";
        String cmdlineWithoutSpace = ">>help";
        String prefix = ">>";
        String await = "help";

        EventsListener eventsListener = new EventsListener();

        Assertions.assertEquals(await, eventsListener.getCmdlineWithoutPrefix(prefix, cmdlineWithoutSpace));
        Assertions.assertEquals(await, eventsListener.getCmdlineWithoutPrefix(prefix, cmdlineWithSpace));
    }
}

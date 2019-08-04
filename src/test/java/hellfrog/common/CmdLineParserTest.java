package hellfrog.common;

import hellfrog.core.EventsListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class CmdLineParserTest {

    @Test
    public void testPrefixCut() {
        String cmdlineWithSpace = ">> help";
        String cmdlineWithoutSpace = ">>help";
        String prefix = ">>";
        String await = "help";

        EventsListener eventsListener = new EventsListener();

        Assertions.assertEquals(await, eventsListener.getCmdlineWithoutPrefix(prefix, cmdlineWithoutSpace));
        Assertions.assertEquals(await, eventsListener.getCmdlineWithoutPrefix(prefix, cmdlineWithSpace));
    }
}

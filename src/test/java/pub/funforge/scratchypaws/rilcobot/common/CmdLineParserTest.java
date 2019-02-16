package pub.funforge.scratchypaws.rilcobot.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pub.funforge.scratchypaws.rilcobot.core.CmdLineParser;

public class CmdLineParserTest {

    @Test
    public void testPrefixCut() throws Exception {
        String cmdlineWithSpace = ">> help";
        String cmdlineWithoutSpace = ">>help";
        String prefix = ">>";
        String await = "help";

        CmdLineParser cmdLineParser = new CmdLineParser();

        Assertions.assertEquals(await, cmdLineParser.getCmdlineWithoutPrefix(prefix, cmdlineWithoutSpace));
        Assertions.assertEquals(await, cmdLineParser.getCmdlineWithoutPrefix(prefix, cmdlineWithSpace));
    }
}

package hellfrog.core;

import hellfrog.common.CommonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerSideResolverTest {

    @Test
    public void testCustomEmojiSplit() {
        String one = "<:12:523648474466746369>";
        String two = "<:tumblr_nr1455TpKV1sh290co2_540:52364847341371920>";

        long awaitOne = 523648474466746369L;
        long awaitTwo = 52364847341371920L;

        Assertions.assertEquals(awaitOne,
                CommonUtils.onlyNumbersToLong(ServerSideResolver.getCustomEmojiRawId(one)));
        Assertions.assertEquals(awaitTwo,
                CommonUtils.onlyNumbersToLong(ServerSideResolver.getCustomEmojiRawId(two)));
    }
}

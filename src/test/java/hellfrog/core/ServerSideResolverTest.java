package hellfrog.core;

import hellfrog.common.CommonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerSideResolverTest {

    @Test
    public void customEmojiSplitTest() {
        String one = "<:12:523648474466746369>";
        String two = "<:tumblr_nr1455TpKV1sh290co2_540:52364847341371920>";

        long awaitOne = 523648474466746369L;
        long awaitTwo = 52364847341371920L;

        Assertions.assertEquals(awaitOne,
                CommonUtils.onlyNumbersToLong(ServerSideResolver.getCustomEmojiRawId(one)));
        Assertions.assertEquals(awaitTwo,
                CommonUtils.onlyNumbersToLong(ServerSideResolver.getCustomEmojiRawId(two)));
    }

    @Test
    public void quoteEveryoneTagsTest() {
        Assertions.assertEquals("", ServerSideResolver.quoteEveryoneTags(null));
        Assertions.assertEquals("", ServerSideResolver.quoteEveryoneTags(""));
        Assertions.assertEquals("Hello here `@everyone`", ServerSideResolver.quoteEveryoneTags("Hello here @everyone"));
        Assertions.assertEquals("Hello here \\ `@everyone`", ServerSideResolver.quoteEveryoneTags("Hello here \\@everyone"));
        Assertions.assertEquals("Hello here `@everyone`", ServerSideResolver.quoteEveryoneTags("Hello here `@everyone`"));
        Assertions.assertEquals("Hello here `@everyone`", ServerSideResolver.quoteEveryoneTags("Hello here`@everyone`"));
        Assertions.assertEquals(" `@everyone` `@everyone` `@here`", ServerSideResolver.quoteEveryoneTags("@everyone@everyone@here"));
    }
}

package hellfrog;

import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EmojiTests {

    @Test
    public void testEmoji() {
        String emoji = "\uD83E\uDD8A";
        String twoEmoji = "\uD83E\uDD8A❤";
        String nonEmoji = "some";

        Assertions.assertEquals("", EmojiParser.removeAllEmojis(emoji));
        Assertions.assertEquals(nonEmoji, EmojiParser.removeAllEmojis(nonEmoji));

        Assertions.assertTrue(EmojiManager.isEmoji(emoji));
        Assertions.assertEquals(1, EmojiParser.extractEmojis(emoji).size());
        Assertions.assertEquals(2, EmojiParser.extractEmojis(twoEmoji).size());
    }
}

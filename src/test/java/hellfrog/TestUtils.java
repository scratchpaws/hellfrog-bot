package hellfrog;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class TestUtils {

    private static final String CHAR_SEQ = "ABCDEFGHIJKLKMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "01234567890" +
            "<>!@\"#$%^&*()[]|^:;-_+=\\/.,`~";

    public static long randomDiscordEntityId() {
        return ThreadLocalRandom.current()
                .nextLong(100000000000000000L, 999999999999999999L);
    }

    @NotNull
    public static String randomStringName(int length) {
        StringBuilder buffer = new StringBuilder(length);
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            buffer.append(CHAR_SEQ.charAt(threadLocalRandom.nextInt(CHAR_SEQ.length())));
        }
        return buffer.toString();
    }
}

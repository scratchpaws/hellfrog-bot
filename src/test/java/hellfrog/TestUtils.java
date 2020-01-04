package hellfrog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
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

    @NotNull
    public static List<Long> randomDiscordEntitiesIds(int minCount, int maxCount) {
        int entitiesCount = ThreadLocalRandom.current().nextInt(minCount, maxCount + 1);
        List<Long> tmp = new ArrayList<>(entitiesCount);
        for (int i = 0; i < entitiesCount; i++) {
            while (true) {
                long discordEntityId = randomDiscordEntityId();
                if (tmp.contains(discordEntityId)) {
                    continue;
                }
                tmp.add(discordEntityId);
                break;
            }
        }
        return Collections.unmodifiableList(tmp);
    }

    @NotNull
    public static List<Long> randomDiscordEntitiesIds(int maxCount) {
        return randomDiscordEntitiesIds(0, maxCount);
    }

    @NotNull
    public static List<String> randomNames(int minCount, int maxCount, int length) {
        int entitiesCount = ThreadLocalRandom.current().nextInt(minCount, maxCount + 1);
        List<String> tmp = new ArrayList<>(entitiesCount);
        for (int i = 0; i < entitiesCount; i++) {
            while (true) {
                String randomName = randomStringName(length);
                if (tmp.contains(randomName)) {
                    continue;
                }
                tmp.add(randomName);
                break;
            }
        }
        return Collections.unmodifiableList(tmp);
    }

    public static <T> List<T> randomSublist(@Nullable List<T> origin, int minCount, int maxCount) {
        if (origin == null || origin.isEmpty()) {
            return Collections.emptyList();
        }
        int min = Math.min(minCount, origin.size());
        min = Math.max(min, 0);
        int max = Math.min(maxCount, origin.size());
        max = Math.max(max, 0);
        ThreadLocalRandom tlrand = ThreadLocalRandom.current();
        int count = tlrand.nextInt(Math.min(min, max), Math.max(min, max) + 1);
        List<T> temp;
        if (count == origin.size()) {
            temp = new ArrayList<>(origin);
            Collections.shuffle(temp);
        } else {
            temp = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                while (true) {
                    T value = origin.get(tlrand.nextInt(0, origin.size()));
                    if (!temp.contains(value)) {
                        temp.add(value);
                        break;
                    }
                }
            }
        }
        return Collections.unmodifiableList(temp);
    }

    @Test
    public void randomSublistSelfTest() {
        List<Long> randomItems = randomDiscordEntitiesIds(50, 100);
        // при подаче null всегда возвращается пустой лист
        Assertions.assertTrue(randomSublist(null, 0, 0).isEmpty());
        List<Long> sublist = randomSublist(randomItems, 50, 50);
        // Запрашивали 50 случайных элементов
        Assertions.assertEquals(50, sublist.size());
        // ... которые существуют в основном списке
        for (long item : sublist) {
            Assertions.assertTrue(sublist.contains(item));
        }
        // И все уникальны
        TreeSet<Long> uniqes = new TreeSet<>(sublist);
        Assertions.assertEquals(50, uniqes.size());

        // Генерация произвольных подсписков, входящих в указанный диапазон размерности
        // с аналогичными выше проверками на соответствие элементам основного списка
        // и уникальностью
        sublist = randomSublist(randomItems, 5, 10);
        Assertions.assertTrue(sublist.size() >= 5 && sublist.size() <= 10);
        for (long item : sublist) {
            Assertions.assertTrue(sublist.contains(item));
        }
        uniqes = new TreeSet<>(sublist);
        Assertions.assertEquals(uniqes.size(), sublist.size());
        sublist = randomSublist(randomItems, 10, 5);
        Assertions.assertTrue(uniqes.size() >= 5 && uniqes.size() <= 10);
        for (long item : sublist) {
            Assertions.assertTrue(sublist.contains(item));
        }
        uniqes = new TreeSet<>(sublist);
        Assertions.assertEquals(uniqes.size(), sublist.size());
    }
}

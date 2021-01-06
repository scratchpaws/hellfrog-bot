package hellfrog;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TestUtils {

    private static final String CHAR_SEQ = "ABCDEFGHIJKLKMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "01234567890" +
            "<>!@\"#$%^&*()[]|^:;-_+=\\/.,`~";

    private static final String URI_CHAR_SEQ = "abcdefghijklmnopqrstuvwxyz" +
            "01234567890";

    public static long randomDiscordEntityId() {
        return ThreadLocalRandom.current()
                .nextLong(100000000000000000L, 999999999999999999L);
    }

    private static final long INSTANT_MILLIS_MIN = Instant.EPOCH.toEpochMilli();
    private static final long INSTANT_MILLIS_MAX = Instant.now().toEpochMilli();

    @NotNull
    public static String randomStringName(int length) {
        return randomChars(length, CHAR_SEQ);
    }

    public static @NotNull String randomURIEntryName(int length) {
        return randomChars(length, URI_CHAR_SEQ);
    }

    private static @NotNull String randomChars(int length, @NotNull String charSeq) {
        StringBuilder buffer = new StringBuilder(length);
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            buffer.append(charSeq.charAt(threadLocalRandom.nextInt(charSeq.length())));
        }
        return buffer.toString();
    }

    @NotNull
    public static @UnmodifiableView List<Long> randomDiscordEntitiesIds(int minCount, int maxCount) {
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
    public static @UnmodifiableView List<Long> randomDiscordEntitiesIds(int maxCount) {
        return randomDiscordEntitiesIds(0, maxCount);
    }

    @NotNull
    public static @UnmodifiableView List<String> randomNames(int minCount, int maxCount, int length) {
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

    public static URI randomURI() {
        URIBuilder uriBuilder = new URIBuilder();
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        if (tlr.nextBoolean()) {
            uriBuilder.setScheme("https");
        } else {
            uriBuilder.setScheme("http");
        }
        StringBuilder hostName = new StringBuilder();
        int hostNameParts = tlr.nextInt(2, 6);
        for (int i = 1; i <= hostNameParts; i++) {
            int entryLength = tlr.nextInt(3, 16);
            hostName.append(randomURIEntryName(entryLength));
            if (i < hostNameParts) {
                hostName.append('.');
            }
        }
        uriBuilder.setHost(hostName.toString());
        StringBuilder path = new StringBuilder();
        int pathParts = tlr.nextInt(6);
        for (int i = 1; i <= pathParts; i++) {
            int entryLength = tlr.nextInt(3, 16);
            path.append(randomURIEntryName(entryLength));
            if (i < pathParts) {
                path.append('/');
            }
        }
        if (tlr.nextBoolean()) {
            path.append('/');
        }
        uriBuilder.setPath(path.toString());
        if (tlr.nextBoolean()) {
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            int paramCount = tlr.nextInt(1, 5);
            for (int i = 1; i <= paramCount; i++) {
                int paramLength = tlr.nextInt(2, 11);
                int valueLength = tlr.nextInt(2, 11);
                String paramName = randomURIEntryName(paramLength);
                String paramValue = null;
                if (tlr.nextBoolean()) {
                    paramValue = randomURIEntryName(valueLength);
                }
                NameValuePair pair = new BasicNameValuePair(paramName, paramValue);
                nameValuePairs.add(pair);
            }
            uriBuilder.setParameters(nameValuePairs);
        }
        try {
            return uriBuilder.build();
        } catch (Exception err) {
            return URI.create("https://www.example.com");
        }
    }

    public static TimeZone randomTimeZone() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        String[] allIds = TimeZone.getAvailableIDs();
        String selectedId = allIds[tlr.nextInt(0, allIds.length)];
        return TimeZone.getTimeZone(selectedId);
    }

    public static Instant randomInstant() {
        ThreadLocalRandom tlr = ThreadLocalRandom.current();
        return Instant.ofEpochMilli(tlr.nextLong(INSTANT_MILLIS_MIN, INSTANT_MILLIS_MAX));
    }

    public static<E> void assertEmpty(@Nullable Collection<E> collection, @NotNull String message) {
        Assertions.assertTrue(collection != null && collection.isEmpty(), message);
    }

    public static<E> void assertEmpty(@Nullable Collection<E> collection) {
        Assertions.assertTrue(collection != null && collection.isEmpty());
    }

    public static<E> void assertNullOrEmpty(@Nullable Collection<E> collection) {
        Assertions.assertTrue(collection == null || collection.isEmpty());
    }

    public static<E> void assertNullOrEmpty(@Nullable Collection<E> collection, @NotNull String message) {
        Assertions.assertTrue(collection == null || collection.isEmpty());
    }
}

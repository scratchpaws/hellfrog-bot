package hellfrog.common;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class CommonUtils {

    @Contract("null -> true")
    public static boolean isTrStringEmpty(CharSequence str) {
        return str == null || str.toString().trim().isEmpty();
    }

    @Contract("null -> false")
    public static boolean isTrStringNotEmpty(@Nullable CharSequence str) {
        return str != null && !str.toString().trim().isEmpty();
    }

    @NotNull
    public static String cutLeftString(@NotNull String source, @NotNull String cut) {
        if (source.length() >= cut.length() && source.startsWith(cut) && source.length() >= 1 && cut.length() >= 1) {
            return source.substring(cut.length());
        } else {
            return source;
        }
    }

    @NotNull
    public static String cutRightString(@NotNull String source, @NotNull String cut) {
        if (source.length() >= cut.length() && source.endsWith(cut) && source.length() >= 1 && cut.length() >= 1) {
            return source.substring(0, source.length() - cut.length());
        } else {
            return source;
        }
    }

    public static boolean isLong(String rawValue) {
        try {
            Long.parseLong(rawValue);
            return true;
        } catch (NullPointerException | NumberFormatException err) {
            return false;
        }
    }

    public static List<String> nullableArrayToNonNullList(String[] array) {
        List<String> result = new ArrayList<>();
        if (array != null && array.length > 0)
            result = Arrays.asList(array);
        return result;
    }

    public static long onlyNumbersToLong(String value) {
        if (isTrStringEmpty(value)) return 0;
        String cleared = value.replaceAll("[^\\d]", "");
        try {
            return Long.parseLong(cleared);
        } catch (NumberFormatException err) {
            return 0;
        }
    }

    public static TimeZone getUtcTimeZone() {
        return TimeZone.getTimeZone("UTC");
    }

    public static ZoneId getUtcZoneId() {
        return ZoneId.of("UTC");
    }

    public static String getCurrentGmtTimeAsString() {
        Calendar current = Calendar.getInstance(getUtcTimeZone());
        return String.format("%tF %<tT (UTC)", current);
    }

    public static String getCurrentGmtTimeAsNewlineString() {
        Calendar current = Calendar.getInstance(getUtcTimeZone());
        return String.format("%tF\n%<tT (UTC)", current);
    }

    public static long getCurrentGmtTimeAsMillis() {
        Calendar current = Calendar.getInstance(getUtcTimeZone());
        return current.getTimeInMillis();
    }

    @Contract(pure = true)
    public static long getLowValue(long maxValue) {
        if (maxValue >= 2) {
            long lowValue = maxValue / 2;
            if (lowValue / 2 >= 1)
                lowValue /= 2;
            return lowValue;
        } else {
            return 0;
        }
    }

    @Contract(pure = true)
    public static long getHighValue(long maxValue) {
        if (maxValue >= 2) {
            if (maxValue == 2) return 2;
            return maxValue - getLowValue(maxValue);
        } else {
            return maxValue;
        }
    }

    @NotNull
    public static List<String> splitEqually(@NotNull String text, int size) {
        List<String> ret = new ArrayList<>((text.length() + size - 1) / size);

        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }

    @NotNull
    public static String addLinebreaks(String input, int maxLineLength) {
        StringTokenizer tok = new StringTokenizer(input, " ");
        StringBuilder output = new StringBuilder(input.length());
        int lineLen = 0;
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();

            if (lineLen + 1 + word.length() > maxLineLength) {
                output.append("\n");
                lineLen = 0;
            } else {
                if (lineLen > 0) output.append(' ');
            }
            output.append(word);
            lineLen += word.length();
        }
        return output.toString();
    }

    public static boolean safeEqualsTrimStr(String that, String then) {
        return !CommonUtils.isTrStringEmpty(that)
                && !CommonUtils.isTrStringEmpty(then)
                && that.trim().equals(then.trim());
    }

    public static long getLatestDate(Instant entityDateTime, long lastDateOfCalendar) {
        Calendar cl = instantToCalendar(entityDateTime);
        if (lastDateOfCalendar >= 0) {
            Calendar current = Calendar.getInstance(getUtcTimeZone());
            current.setTimeInMillis(lastDateOfCalendar);
            if (cl.after(current)) {
                return cl.getTimeInMillis();
            } else {
                return lastDateOfCalendar;
            }
        } else {
            return cl.getTimeInMillis();
        }
    }

    public static Calendar instantToCalendar(Instant instant) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, getUtcZoneId());
        return GregorianCalendar.from(zdt);
    }

    public static <T> Optional<T> getFirstOrEmpty(@Nullable Collection<T> collection) {
        if (collection == null || collection.isEmpty())
            return Optional.empty();

        for (T entry : collection) {
            return Optional.ofNullable(entry);
        }
        return Optional.empty();
    }

    // https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    public static String humanReadableByteCount(long bytes, boolean si) {
        if (si) {
            if (-1000 < bytes && bytes < 1000) {
                return bytes + " B";
            }
            CharacterIterator ci = new StringCharacterIterator("kMGTPE");
            while (bytes <= -999_950 || bytes >= 999_950) {
                bytes /= 1000;
                ci.next();
            }
            return String.format("%.1f %cB", bytes / 1000.0, ci.current());
        } else {
            long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
            if (absB < 1024) {
                return bytes + " B";
            }
            long value = absB;
            CharacterIterator ci = new StringCharacterIterator("KMGTPE");
            for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
                value >>= 10;
                ci.next();
            }
            value *= Long.signum(bytes);
            return String.format("%.1f %ciB", value / 1024.0, ci.current());
        }
    }

    @NotNull
    @Contract(pure = true)
    public static String reduceConcat(final @Nullable String s1, final @Nullable String s2) {
        return (s1 != null ? s1 : "(?)")
                + ", "
                + (s2 != null ? s2 : "(?)");
    }

    @NotNull
    @Contract(pure = true)
    public static String reduceNewLine(final @Nullable String s1, final @Nullable String s2) {
        return (s1 != null ? s1 : "(?)")
                + "\n"
                + (s1 != null ? s2 : "(?)");
    }

    @Contract(value = "null, _ -> false; !null, null -> false", pure = true)
    @SafeVarargs
    public static<T> boolean in(T that, T... there) {
        if (that == null || there == null) return false;
        for (T item : there) {
            if (that.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public static @NotNull String quoteIfNotPresent(final @Nullable CharSequence str,
                                                    final @Nullable CharSequence that,
                                                    final char quoteChar) {
        if (isTrStringEmpty(str)) {
            return "";
        }
        if (isTrStringEmpty(that)) {
            return str.toString();
        }
        String thatStr = that.toString();
        StringBuilder result = new StringBuilder(str);
        int pos = 0;
        int prevAt;
        int postAt;
        char prev;
        char post;
        while ((pos = result.indexOf(thatStr, pos)) >= 0) {
            prevAt = pos - 1;
            postAt = pos + thatStr.length();
            if (prevAt >= 0 && prevAt < result.length() && postAt >= 0 && postAt < result.length()) {
                prev = result.charAt(prevAt);
                post = result.charAt(postAt);
                if (prev == quoteChar && post == quoteChar) {
                    pos = postAt; // already done
                    continue;
                }
            }
            result.insert(pos, quoteChar);
            pos += 1 + thatStr.length();
            result.insert(pos, quoteChar);
        }
        return result.toString();
    }

    public static @NotNull String prefixIfNotPresent(final @Nullable CharSequence str,
                                                     final @Nullable CharSequence that,
                                                     final char prefix) {
        if (isTrStringEmpty(str)) {
            return "";
        }
        if (isTrStringEmpty(that)) {
            return str.toString();
        }
        String thatStr = that.toString();
        StringBuilder result = new StringBuilder(str);
        int pos = 0;
        int prevAt;
        char prev;
        while ((pos = result.indexOf(thatStr, pos)) >= 0) {
            prevAt = pos - 1;
            if (prevAt >= 0 && prevAt < result.length()) {
                prev = result.charAt(prevAt);
                if (prev == prefix) {
                    pos += thatStr.length();
                    continue;
                }
            }
            result.insert(pos, prefix);
            pos += 1 + thatStr.length();
        }
        return result.toString();
    }
}

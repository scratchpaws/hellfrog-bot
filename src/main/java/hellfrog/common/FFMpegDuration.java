package hellfrog.common;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFMpegDuration
        implements Comparable<FFMpegDuration> {

    private static final Pattern DURATION_PATTERN = Pattern.compile("\\d\\d:\\d\\d:\\d\\d\\.\\d\\d");

    private final int hours;
    private final int minutes;
    private final int seconds;
    private final int milliseconds;
    private final long totalMillis;

    private FFMpegDuration(int hours, int minutes, int seconds, int milliseconds) {
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.milliseconds = milliseconds;
        this.totalMillis = (hours * 60 * 60 + minutes * 60 + seconds) * 100 + milliseconds;
    }

    public static FFMpegDuration parseDuration(@NotNull final String rawDuration) {
        final Matcher durationMatcher = DURATION_PATTERN.matcher(rawDuration);
        if (!durationMatcher.find()) {
            String errMsg = String.format("value \"%s\" is not accepted by \"%s\"", rawDuration,
                    DURATION_PATTERN.pattern());
            throw new IllegalArgumentException(errMsg);
        }
        final String[] rawParts = durationMatcher.group().split("[:.]");
        if (rawParts.length < 4) {
            String errMsg = String.format("value \"%s\" is not accepted by \"%s\"", rawDuration,
                    DURATION_PATTERN.pattern());
            throw new IllegalArgumentException(errMsg);
        }
        final int hours = Integer.parseInt(rawParts[0]);
        final int minutes = Integer.parseInt(rawParts[1]);
        final int seconds = Integer.parseInt(rawParts[2]);
        final int milliseconds = Integer.parseInt(rawParts[3]);
        return new FFMpegDuration(hours, minutes, seconds, milliseconds);
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getMilliseconds() {
        return milliseconds;
    }

    public long getTotalMillis() {
        return totalMillis;
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, milliseconds);
    }

    @Override
    public int compareTo(@NotNull FFMpegDuration o) {
        return Long.compare(this.totalMillis, o.totalMillis);
    }

    public static int compare(@NotNull FFMpegDuration first, @NotNull FFMpegDuration second) {
        return Long.compare(first.totalMillis, second.totalMillis);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FFMpegDuration that = (FFMpegDuration) o;
        return hours == that.hours &&
                minutes == that.minutes &&
                seconds == that.seconds &&
                milliseconds == that.milliseconds &&
                totalMillis == that.totalMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hours, minutes, seconds, milliseconds, totalMillis);
    }
}

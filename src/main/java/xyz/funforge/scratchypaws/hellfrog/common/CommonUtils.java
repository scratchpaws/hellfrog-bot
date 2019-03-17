package xyz.funforge.scratchypaws.hellfrog.common;

import org.jetbrains.annotations.Contract;

import java.util.*;

public class CommonUtils {

    @Contract("null -> true")
    public static boolean isTrStringEmpty(CharSequence str) {
        return str == null || str.toString().trim().length() == 0;
    }

    public static String cutLeftString(String source, String cut) {
        if (source.length() >= cut.length() && source.startsWith(cut) && source.length() >= 1 && cut.length() >= 1) {
            return source.substring(cut.length());
        } else {
            return source;
        }
    }

    public static String cutRightString(String source, String cut) {
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

    public static String getCurrentGmtTimeAsString() {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        Calendar current = Calendar.getInstance(tz);
        return String.format("%tF %<tT (GMT)", current);
    }

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

    public static long getHighValue(long maxValue) {
        if (maxValue >= 2) {
            if (maxValue == 2) return 2;
            return maxValue - getLowValue(maxValue);
        } else {
            return maxValue;
        }
    }

    public static List<String> splitEqually(String text, int size) {
        List<String> ret = new ArrayList<>((text.length() + size - 1) / size);

        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }

    public static boolean safeEqualsTrimStr(String that, String then) {
        return !CommonUtils.isTrStringEmpty(that)
                && !CommonUtils.isTrStringEmpty(then)
                && that.trim().equals(then.trim());
    }
}

package besus.utils;


import besus.utils.func.Func;
import besus.utils.func.Mapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import rx.functions.Func0;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static besus.utils.PredicateEx.*;

/**
 * Created by user on 3/9/2017.
 */
@SuppressWarnings("WeakerAccess")
public class MiscUtils {

    public static String urlencode(String stringToEncode) {
        try {
            if (stringToEncode != null) {
                stringToEncode = URLEncoder.encode(stringToEncode, "UTF-8");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return stringToEncode;
    }

    public static String urldecode(String stringToDecode) {
        try {
            if (stringToDecode != null) {
                stringToDecode = URLDecoder.decode(stringToDecode, "UTF-8");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return stringToDecode;
    }

//    public static String toBase64(byte[] src) {
//        try {
//            return new Base64().encodeToString(src);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static byte[] fromBase64(String src) {
//        try {
//            return new Base64().decode(src);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            result += Integer.parseInt(ipAddressInArray[i]) % 256 * Math.pow(256, power);
        }
        return result;
    }

    public static String longToIp(long ip) {
        StringBuilder result = new StringBuilder(15);
        for (int i = 0; i < 4; i++) {
            result.insert(0, Long.toString(ip & 0xff));
            if (i < 3) {
                result.insert(0, '.');
            }
            ip = ip >> 8;
        }
        return result.toString();
    }

    public static <L, R> Pair<L, R> pair(L left, R right) {
        return new ImmutablePair<>(left, right);
    }

    public static String normallizeVer(String ver) {
        return ver == null ? "4.1" : Arrays.stream(ver.replaceAll("(^\\.*|\\.*$|[^\\d.])", "").split("\\."))
                .map(part -> StringUtils.truncate(part, 3))
                .map(part -> StringUtils.leftPad(part, 3, '0'))
                .reduce((concat, next) -> concat + "." + next).orElse("000");
    }

    public static String shorterVer(String ver) {
        return ver.replaceAll("\\.0*", ".").replaceAll("^0*", "");
    }

    public static int compare(Number n1, Number n2) {
        return Optional.of(Long.compare(n1.longValue(), n2.longValue()))
                .filter(PredicateEx.not(isZero))
                .orElse(Double.compare(n1.doubleValue(), n2.doubleValue()));
    }

    public static Number anyToNum(Object from) {
        if (from == null) {
            return 0;
        }
        return ifInstance(Number.class).select(from,
                num -> (Number) num,
                notNum -> ifInstance(Boolean.class).select(notNum,
                        bool -> ((Boolean) bool) ? 1 : 0,
                        other -> NumberUtils.createNumber(String.valueOf(Optional.ofNullable(other).map(Objects::toString).filter(NumberUtils::isCreatable).orElse("0")))).get())
                .orElse(0);
    }

    @SafeVarargs
    public static <T> T coalesceBy(Predicate<? super T> accept, T... items) {
        for (T item : items) {
            if (accept.test(item)) return item;
        }
        return null;
    }

//    public static JsonObject internStrings(JsonObject src) {
//        return src.stream().collect(JsonObject::new, (res, e) -> res.put(e.getKey().intern(), intern(e.getValue())), JsonObject::mergeIn);
//    }
//
//    public static Object intern(Object value) {
//        if (value instanceof String) {
//            return ((String) value).intern();
//        } else if (value instanceof JsonObject) {
//            return internStrings((JsonObject) value);
//        } else if (value instanceof JsonArray) {
//            return internStrings((JsonArray) value);
//        } else {
//            return value;
//        }
//    }
//
//    public static JsonArray internStrings(JsonArray src) {
//        return src.stream().collect(JsonArray::new, (res, e) -> res.add(intern(e)), JsonArray::addAll);
//    }

    @SafeVarargs
    public static <T> void execForAll(Func.AnyAction<T> action, T... targets) {
        Stream.of(targets).filter(PredicateEx.not(isNull)).forEach(action);
    }

    @SafeVarargs
    public static <T> T coalesceByf(Predicate<? super T> accept, Func0<T>... items) {
        for (Func0<T> item : items) {
            T val = item.call();
            if (accept.test(val)) {
                return val;
            }
        }
        return null;
    }

    public static Timestamp now() {
        return new Timestamp(new Date().getTime());
    }

    @SafeVarargs
    public static <T> T coalesce(T... items) {
        return coalesceBy(PredicateEx.not(isNull), items);
    }

    @SafeVarargs
    public static <T> T coalescef(Func0<T>... items) {
        return coalesceByf(PredicateEx.not(isNull), items);
    }

    public static String underline(CharSequence source) {
        return source.chars().collect(StringBuilder::new, (b, n) -> b.append((char) n).append((char) 0x332), StringBuilder::append).toString();
    }

    public static boolean contains(String s, String regex) {
        return Pattern.compile(regex).matcher(s).find();
    }


    public static int rnd() {
        return new Random().nextInt();
    }


    public static String dashToCamelCase(String cc, boolean firstCap) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < cc.length(); i++) {
            char ch = cc.charAt(i);
            if (i == 0 && ch != '-' && firstCap) {
                ret.append(Character.toUpperCase(ch));
            } else if (ch == '-') {
                if (i < cc.length() - 1) {
                    ret.append(Character.toUpperCase(cc.charAt(++i)));
                }
            } else {
                ret.append(ch);
            }
        }
        return ret.toString();
    }

    public static String camelCaseToDash(String cc) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < cc.length(); i++) {
            char ch = cc.charAt(i);
            if (i == 0) {
                ret.append(Character.toLowerCase(ch));
            } else if (Character.isUpperCase(ch) || (Character.isDigit(ch) && Character.isLetter(cc.charAt(i - 1)))) {
                ret.append('-');
                ret.append(Character.toLowerCase(ch));
            } else {
                ret.append(ch);
            }
        }
        return ret.toString();
    }

    public static String underlineToDash(String cc) {
        return cc.replace('_', '-');
    }

    public static String loggerNameToFileName(String name) {
        return underlineToDash(camelCaseToDash(name.replace("LoggerFile", "")));
    }

    public static String replace(String pattern, Mapper<String, ?> valueExtractor) {
        return StrSubstitutor.replace(pattern, valueExtractor.asMap());
    }
}

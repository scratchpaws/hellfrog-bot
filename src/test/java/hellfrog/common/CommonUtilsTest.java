package hellfrog.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

public class CommonUtilsTest {

    @Test
    public void trStringEmptyTest() {
        String input1 = null;
        String input2 = "";
        String input3 = " \t ";

        Assertions.assertTrue(CommonUtils.isTrStringEmpty(input1));
        Assertions.assertTrue(CommonUtils.isTrStringEmpty(input2));
        Assertions.assertTrue(CommonUtils.isTrStringEmpty(input3));

        Assertions.assertFalse(CommonUtils.isTrStringNotEmpty(input1));
        Assertions.assertFalse(CommonUtils.isTrStringNotEmpty(input2));
        Assertions.assertFalse(CommonUtils.isTrStringNotEmpty(input3));

        input1 = "one";
        input2 = " two ";
        input3 = " three\t";

        Assertions.assertTrue(CommonUtils.isTrStringNotEmpty(input1));
        Assertions.assertTrue(CommonUtils.isTrStringNotEmpty(input2));
        Assertions.assertTrue(CommonUtils.isTrStringNotEmpty(input3));

        Assertions.assertFalse(CommonUtils.isTrStringEmpty(input1));
        Assertions.assertFalse(CommonUtils.isTrStringEmpty(input2));
        Assertions.assertFalse(CommonUtils.isTrStringEmpty(input3));
    }

    @Test
    public void cutLeftStringTest() {
        String input = "r!b one two";
        String cut = "r!b";
        String await = " one two";
        Assertions.assertEquals(await, CommonUtils.cutLeftString(input, cut));

        input = "one";
        cut = input;
        await = "";
        Assertions.assertEquals(await, CommonUtils.cutLeftString(input, cut));
    }

    @Test
    public void cutRightStringTest() {
        String input = "some ome";
        String cut = "ome";
        String await = "some ";
        Assertions.assertEquals(await, CommonUtils.cutRightString(input, cut));

        input = "one";
        cut = input;
        await = "";
        Assertions.assertEquals(await, CommonUtils.cutRightString(input, cut));
    }

    @Test
    public void onlyNumberTest() {
        String input = "<@!516251605864153099>";
        long await = 516251605864153099L;
        Assertions.assertEquals(await, CommonUtils.onlyNumbersToLong(input));
    }

    @Test
    public void testLowValue() {
        Assertions.assertEquals(5L, CommonUtils.getLowValue(20L));
        Assertions.assertEquals(1L, CommonUtils.getLowValue(2L));
        Assertions.assertEquals(1L, CommonUtils.getLowValue(5L));
        Assertions.assertEquals(1L, CommonUtils.getLowValue(6L));
        Assertions.assertEquals(2L, CommonUtils.getLowValue(10L));
    }

    @Test
    public void testHighValue() {
        Assertions.assertEquals(15L, CommonUtils.getHighValue(20L));
        Assertions.assertEquals(2L, CommonUtils.getHighValue(2L));
        Assertions.assertEquals(4L, CommonUtils.getHighValue(5L));
        Assertions.assertEquals(5L, CommonUtils.getHighValue(6L));
        Assertions.assertEquals(8L, CommonUtils.getHighValue(10L));
    }

    @Test
    public void testEquals() {
        Assertions.assertFalse(CommonUtils.safeEqualsTrimStr(null, null));
        Assertions.assertTrue(CommonUtils.safeEqualsTrimStr("all", "all"));
        Assertions.assertTrue(CommonUtils.safeEqualsTrimStr(" all ", "all"));
    }

    @Test
    public void latestDateTimeTest() {
        final String dateTimeFormatter = "%tF %<tT.%<tL";
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        System.err.println(String.format(dateTimeFormatter, calendar));
        Instant instant = Instant.now();
        System.err.println(String.format(dateTimeFormatter, CommonUtils.instantToCalendar(instant)));

        long result = CommonUtils.getLatestDate(instant, calendar.getTimeInMillis());
        long instantAsCalendarToLong = CommonUtils.instantToCalendar(instant).getTimeInMillis();

        Assertions.assertEquals(result, instantAsCalendarToLong);
    }

    @Test
    public void firstOrEmptyTest() {
        List<String> empty = Collections.emptyList();
        List<String> nulled = null;
        List<String> once = new ArrayList<>();
        List<String> multiple = new ArrayList<>();

        String one = "one";
        String two = "two";

        once.add(one);
        multiple.add(one);
        multiple.add(two);

        Optional<String> ofEmpty = CommonUtils.getFirstOrEmpty(empty);
        Optional<String> ofNulled = CommonUtils.getFirstOrEmpty(nulled);
        Optional<String> ofOnce = CommonUtils.getFirstOrEmpty(once);
        Optional<String> ofMultiple = CommonUtils.getFirstOrEmpty(multiple);

        Assertions.assertFalse(ofEmpty.isPresent());
        Assertions.assertFalse(ofNulled.isPresent());
        Assertions.assertEquals(one, ofOnce.get());
        Assertions.assertEquals(one, ofMultiple.get());
    }
}

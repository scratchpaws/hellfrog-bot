package xyz.funforge.scratchypaws.hellfrog.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

public class OptionalUtilsTest {

    private static final String value = "test";
    private static final String emptyValue = "empty";
    private static final Optional<String> ofEmpty = Optional.empty();
    private static final Optional<String> ofValue = Optional.of(value);

    @Test
    public void nonEmptyTest() {
        ArrayList<String> secondRes = new ArrayList<>();
        OptionalUtils.ifPresentOrElse(ofValue, secondRes::add, () -> secondRes.add(emptyValue));
        Assertions.assertFalse(secondRes.isEmpty());
        String secondResult = secondRes.get(0);
        Assertions.assertEquals(value, secondResult);
    }

    @Test
    public void emptyTest() {
        ArrayList<String> firstRes = new ArrayList<>();
        OptionalUtils.ifPresentOrElse(ofEmpty, firstRes::add, () -> firstRes.add(emptyValue));
        Assertions.assertFalse(firstRes.isEmpty());
        String firstResult = firstRes.get(0);
        Assertions.assertEquals(emptyValue, firstResult);
    }
}

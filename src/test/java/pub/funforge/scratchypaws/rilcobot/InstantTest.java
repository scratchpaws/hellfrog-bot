package pub.funforge.scratchypaws.rilcobot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class InstantTest {

    @Test
    public void testInstants() {
        Instant current = Instant.now();
        long endDate = current.getEpochSecond();
        int endNano = current.getNano();
        Instant restores = Instant.ofEpochSecond(endDate, endNano);
        Instant future = ChronoUnit.MINUTES.addTo(current, 5);
        Assertions.assertEquals(current, restores);
        Assertions.assertEquals(5, ChronoUnit.MINUTES.between(current, future));
    }
}

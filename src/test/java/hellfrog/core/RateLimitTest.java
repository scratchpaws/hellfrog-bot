package hellfrog.core;

import hellfrog.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RateLimitTest {

    @Test
    public void testUserLimits() {
        long entity = TestUtils.randomDiscordEntityId();

        Logger log = LogManager.getLogger("Test limits");
        boolean gotLimits = false;
        for (int i = 0; i < 100; i++) {
            if (RateLimiter.userIsLimited(entity)) {
                gotLimits = true;
                log.info("User entity limit reached at {}", i);
                break;
            }
        }

        Assertions.assertTrue(gotLimits, "Limit must be reached");
    }

    @Test
    public void testServerLimits() {
        long entity = TestUtils.randomDiscordEntityId();

        Logger log = LogManager.getLogger("Test limits");
        boolean gotLimits = false;
        for (int i = 0; i < 100; i++) {
            if (RateLimiter.serverIsLimited(entity)) {
                gotLimits = true;
                log.info("Server entity limit reached at {}", i);
                break;
            }
        }

        Assertions.assertTrue(gotLimits, "Limit must be reached");
    }

    @Test
    public void testNotifyLimits() {
        long entity = TestUtils.randomDiscordEntityId();

        Logger log = LogManager.getLogger("Test limits");
        boolean gotLimits = false;
        for (int i = 0; i < 100; i++) {
            if (RateLimiter.notifyIsLimited(entity)) {
                gotLimits = true;
                log.info("Notify limit reached at {}", i);
                break;
            }
        }

        Assertions.assertTrue(gotLimits, "Limit must be reached");
    }
}

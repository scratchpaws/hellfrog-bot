package hellfrog.reacts.dice;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class RandomSource {

    private static final long MAX_SOURCES = 12L;
    private final Map<Long, Queue<Long>> prebuildPools;

    private static final RandomSource INSTANCE = new RandomSource();

    private RandomSource() {
        Map<Long, Queue<Long>> pools = new HashMap<>();
        for (long i = 1; i <= MAX_SOURCES; i++) {
            Queue<Long> pool = new ConcurrentLinkedQueue<>();
            pools.put(i, pool);
        }
        this.prebuildPools = Collections.unmodifiableMap(pools);
    }

    public static RandomSource getInstance() {
        return INSTANCE;
    }

    public long getDice(final long numOfFaces) {
        if (numOfFaces > MAX_SOURCES) {
            return ThreadLocalRandom.current().nextLong(1L, numOfFaces + 1);
        }

        if (numOfFaces < 1L) {
            return 0L;
        }

        final Queue<Long> pool = prebuildPools.get(numOfFaces);
        if (pool.isEmpty()) {
            synchronized (prebuildPools.get(numOfFaces)) {
                if (pool.isEmpty()) {
                    final List<Long> values = new ArrayList<>();
                    for (long i = 1; i <= numOfFaces; i++) {
                        for (int j = 0; j < 3; j++) {
                            values.add(i);
                        }
                    }
                    Collections.shuffle(values);
                    pool.addAll(values);
                }
            }
        }

        return Optional.ofNullable(pool.poll())
                .orElse(ThreadLocalRandom.current().nextLong(1L, numOfFaces + 1));
    }
}

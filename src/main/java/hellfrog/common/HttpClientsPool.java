package hellfrog.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpClientsPool
    implements Runnable {

    private final ConcurrentLinkedQueue<SimpleHttpClient> pool = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executorService;
    private static final int MIN_OBJECTS = 3;
    private static final int MAX_OBJECTS = 10;
    private static final long VALIDATION_INTERVAL = 60L;
    private final Logger log = LogManager.getLogger("HTTP clients pool");

    public HttpClientsPool() {
        run();
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(this,
                VALIDATION_INTERVAL, VALIDATION_INTERVAL, TimeUnit.SECONDS);
    }

    public SimpleHttpClient borrowClient() {
        SimpleHttpClient client;
        if ((client = pool.poll()) == null) {
            client = new SimpleHttpClient();
        }
        return client;
    }

    public void returnClient(SimpleHttpClient client) {
        if (client == null) {
            return;
        }
        this.pool.offer(client);
    }

    @Override
    public void run() {
        int size = pool.size();
        log.info("Current size: {}", size);

        if (size < MIN_OBJECTS) {
            int sizeToBeAdded = MIN_OBJECTS + size;
            for (int i = 0; i < sizeToBeAdded; i++) {
                pool.add(new SimpleHttpClient());
            }
        } else if (size > MAX_OBJECTS) {
            int sizeToBeRemoved = size - MAX_OBJECTS;
            for (int i = 0; i < sizeToBeRemoved; i++) {
                SimpleHttpClient client = pool.poll();
                if (client != null) {
                    client.close();
                }
            }
        }
    }

    public void stop() {
        executorService.shutdown();
    }
}

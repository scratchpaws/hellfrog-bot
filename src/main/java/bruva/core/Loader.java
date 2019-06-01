package bruva.core;

import bruva.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class Loader {

    private static final Logger log = LogManager.getLogger(Loader.class.getSimpleName());

    public static void main(String... args) throws Exception {
        log.info("Starting database");
        HibernateUtils.getSessionFactory();
        log.info("Database is started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing database (Shutdown hook)");
            HibernateUtils.close();
            log.info("Database is closed (Shutdown hook)");
        }));

        SettingsController.getInstance().getApiKey().ifPresentOrElse(Loader::start,
                () -> log.error("Required bot api for startup. It placed into file "
                        + SettingsController.getApiKeyFile()));

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(15_000L);
            } catch (InterruptedException err) {
            }
            System.exit(0);
        });
        //HibernateUtils.generateDDL("./ddl.txt");
    }

    private static void start(String apiKey) {
        new DiscordApiBuilder()
                .setToken(apiKey)
                .setRecommendedTotalShards()
                .join()
                .loginAllShards()
                .forEach(shardFuture ->
                        shardFuture.thenAccept(Loader::thenShardConnected)
                                .exceptionally(Loader::thenExceptionReached)
                );
    }

    private static void thenShardConnected(DiscordApi api) {
        log.info("Shard with id " + api.getCurrentShard() + " (of " + api.getTotalShards() + ") connected");
        if (api.getCurrentShard() == 0)
            SettingsController.getInstance().setFirstShard(api);

        long serverId = 575113435524890646L;
        DiscordApi shard = SettingsController.getInstance().getShardForServer(serverId);
        if (shard != null) {
            System.err.println("For server " + serverId + " shard is " + shard.getCurrentShard());
        } else {
            System.err.println("For server " + serverId + " shard is null");
        }
    }

    private static Void thenExceptionReached(Throwable error) {
        log.error(error);
        return null;
    }
}

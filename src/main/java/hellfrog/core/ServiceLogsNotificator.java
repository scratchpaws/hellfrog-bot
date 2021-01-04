package hellfrog.core;

import hellfrog.common.BroadCast;
import hellfrog.settings.SettingsController;
import org.javacord.api.DiscordApi;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServiceLogsNotificator
        implements Runnable {

    private static final int SEVERITY_INFO = 0;
    private static final int SEVERITY_WARN = 1;
    private static final int SEVERITY_ERROR = 2;

    private final ScheduledFuture<?> scheduled;

    public ServiceLogsNotificator() {
        scheduled = Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this, 5L, 5L, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        if (api != null) {
            processQueue(LogsStorage.getErrorMessages(), SEVERITY_ERROR);
            processQueue(LogsStorage.getWarnMessages(), SEVERITY_WARN);
            processQueue(LogsStorage.getInfoMessages(), SEVERITY_INFO);
        }
    }

    private void processQueue(@NotNull final Queue<String> messages, int severity) {
        if (messages.isEmpty()) {
            return;
        }
        BroadCast.MessagesLogger logger = BroadCast.getLogger();
        String message;
        while ((message = messages.poll()) != null) {
            switch (severity) {
                case SEVERITY_ERROR -> logger.addErrorMessage(message);
                case SEVERITY_WARN -> logger.addWarnMessage(message);
                case SEVERITY_INFO -> logger.addInfoMessage(message);
            }
        }
        logger.send();
    }

    public void stop() {
        scheduled.cancel(false);
        while (!scheduled.isCancelled() || !scheduled.isDone()) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException brE) {
                scheduled.cancel(true);
            }
        }
    }
}

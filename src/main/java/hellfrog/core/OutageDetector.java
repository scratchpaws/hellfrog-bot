package hellfrog.core;

import hellfrog.common.CommonConstants;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;

public class OutageDetector
        implements Runnable, CommonConstants {

    private final ScheduledFuture<?> scheduledFuture;
    private final Logger log = LogManager.getLogger(this.getClass().getSimpleName());

    public OutageDetector() {
        ScheduledExecutorService voiceService = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = voiceService.scheduleWithFixedDelay(this, 5L, 5L, TimeUnit.MINUTES);
    }

    @Override
    public void run() {
        CompletableFuture.runAsync(this::detachedThread);
    }

    private void detachedThread() {
        try {
            final SettingsController settingsController = SettingsController.getInstance();
            final long serviceChannelId = settingsController.getMainDBController()
                    .getCommonPreferencesDAO()
                    .getBotServiceChannelId();
            settingsController.getDiscordApi()
                    .getServerTextChannelById(serviceChannelId)
                    .ifPresent(serviceChannel -> {
                        try {
                            serviceChannel.getMessages(100).get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
                        } catch (Exception err) {
                            log.error("Outage detected: {}", err.getMessage());
                            SettingsController.getInstance().shutdown();
                        }
                    });
        } catch (Exception err) {
            log.error("Outage detected: {}", err.getMessage());
            SettingsController.getInstance().shutdown();
        }
    }

    public void stop() {
        scheduledFuture.cancel(true);
        while (!scheduledFuture.isCancelled() || !scheduledFuture.isDone()) {
            try {
                scheduledFuture.wait();
            } catch (InterruptedException brk) {
                scheduledFuture.cancel(true);
            }
        }
    }
}

package hellfrog.core;

import hellfrog.settings.SettingsController;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoSaveSettingsTask
        implements Runnable {

    private final ScheduledFuture<?> scheduled;

    public AutoSaveSettingsTask() {
        scheduled = Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this, 5L, 5L, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        SettingsController settingsController = SettingsController.getInstance();
        settingsController.getServerListWithStatistic()
                .forEach(settingsController::saveServerSideStatistic);
        settingsController.getServerListWithConfig()
                .forEach(settingsController::saveServerSideParameters);
        settingsController.saveCommonPreferences();
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

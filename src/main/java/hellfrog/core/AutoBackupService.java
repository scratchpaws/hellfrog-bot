package hellfrog.core;

import hellfrog.settings.SettingsController;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoBackupService
        implements Runnable {

    private final ScheduledFuture<?> scheduled;

    public AutoBackupService() {
        scheduled = Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this, 1L, 1L, TimeUnit.HOURS);
    }

    @Override
    public void run() {
        SettingsController.getInstance()
                .getMainDBController()
                .createBackup();
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

package xyz.funforge.scratchypaws.hellfrog.core;

import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerStatisticTask
        implements Runnable {

    private ScheduledFuture<?> scheduled;

    public ServerStatisticTask() {
        scheduled = Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this, 5L, 5L, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        SettingsController settingsController = SettingsController.getInstance();
        settingsController.getServerListWithStatistic()
                .forEach(settingsController::saveServerSideStatistic);
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

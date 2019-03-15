package xyz.funforge.scratchypaws.hellfrog.core;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

public class Loader {

    public static void main(String... args) {
        EventsListener eventsListener = new EventsListener();

        new DiscordApiBuilder()
                .setToken(SettingsController.getInstance().getApiKey())
                .login()
                .thenAccept(api -> {
                    api.addMessageCreateListener(eventsListener);
                    api.addMessageEditListener(eventsListener);
                    api.addMessageDeleteListener(eventsListener);
                    api.addReactionAddListener(eventsListener);
                    api.addReactionRemoveListener(eventsListener);
                    api.addReactionRemoveAllListener(eventsListener);
                    api.addServerMemberJoinListener(eventsListener);
                    api.addServerMemberLeaveListener(eventsListener);
                    api.updateActivity(ActivityType.LISTENING, "<prefix> help");
                    SettingsController.getInstance().setDiscordApi(api);
                    api.addServerJoinListener(eventsListener);
                    eventsListener.onReady();
                })
                .exceptionally(Loader::onException);
    }

    private static Void onException(Throwable th) {
        if (th != null) {
            String msg = ExceptionUtils.getStackTrace(th);
            System.out.println(msg);
        }
        return null;
    }
}

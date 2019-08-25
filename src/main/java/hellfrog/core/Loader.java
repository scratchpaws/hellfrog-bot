package hellfrog.core;

import hellfrog.commands.cmdline.BotCommand;
import hellfrog.commands.scenes.Scenario;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.MainDBController;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.jetbrains.annotations.Contract;

public class Loader {

    public static void main(String... args) {
            SettingsController settingsController = SettingsController.getInstance();
            EventsListener eventsListener = new EventsListener();
            BotCommand.all(); // заранее инициируем поиск и инстантинацию классов команд
            MsgCreateReaction.all();
            Scenario.all();

            new DiscordApiBuilder()
                    .setToken(settingsController.getApiKey())
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
                        api.addServerMemberBanListener(eventsListener);
                        api.addServerMemberUnbanListener(eventsListener);
                        api.updateActivity(ActivityType.LISTENING, "<prefix> help");
                        SettingsController.getInstance().setDiscordApi(api);
                        api.addServerJoinListener(eventsListener);
                        eventsListener.onReady();
                    })
                    .exceptionally(Loader::onException);
    }

    @Contract("null -> null")
    private static Void onException(Throwable th) {
        if (th != null) {
            String msg = ExceptionUtils.getStackTrace(th);
            System.out.println(msg);
        }
        return null;
    }
}

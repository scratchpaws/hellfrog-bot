package hellfrog.core;

import hellfrog.commands.cmdline.BotCommand;
import hellfrog.commands.scenes.Scenario;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.InstanceType;
import hellfrog.settings.db.h2.MainDBControllerH2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.jetbrains.annotations.Contract;

public class Loader {

    private static final Logger log = LogManager.getLogger("Core");

    public static void main(String... args) {

        if (args.length > 0) {
            try (MainDBControllerH2 dbControllerH2 = new MainDBControllerH2(InstanceType.TEST)) {
                dbControllerH2.generateDDL(args[0]);
            } catch (Exception err) {
                err.printStackTrace();
            }
            System.exit(0);
        }

        final SettingsController settingsController = SettingsController.getInstance();
        final EventsListener eventsListener = new EventsListener();
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
                    api.addServerLeaveListener(eventsListener);
                    api.addRoleChangePermissionsListener(eventsListener);
                    eventsListener.onReady();
                })
                .exceptionally(Loader::onException);
    }

    @Contract("null -> null")
    private static Void onException(Throwable th) {
        if (th != null) {
            log.error(th);
        }
        return null;
    }
}

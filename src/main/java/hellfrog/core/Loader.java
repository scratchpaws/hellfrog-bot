package hellfrog.core;

import hellfrog.commands.cmdline.BotCommand;
import hellfrog.commands.scenes.Scenario;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.ApiKeyStorage;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.jetbrains.annotations.Contract;

import java.io.Console;
import java.io.IOException;
import java.util.Scanner;

public class Loader {

    private static final Logger log = LogManager.getLogger("Core");

    public static void main(String... args) {

        if (args.length > 0) {
            System.err.print("First cmdline value rewrite api key. Continue? [y/n]> ");
            Console console = System.console();
            String answer;
            if (console != null) {
                answer = console.readLine();
            } else {
                answer = new Scanner(System.in).nextLine();
            }
            if (answer != null && answer.strip().equalsIgnoreCase("y")) {
                try {
                    ApiKeyStorage.writeApiKey(args[0]);
                    System.err.println("Saved");
                } catch (IOException err) {
                    System.err.println(err.getMessage());
                    System.exit(2);
                }
            }
            System.exit(0);
        }

        final SettingsController settingsController = SettingsController.getInstance();
        final EventsListener eventsListener = new EventsListener();
        for (BotCommand cmd : BotCommand.all()) { // заранее инициируем поиск и инстантинацию классов команд
            if (log.isDebugEnabled()) {
                log.debug(cmd.getClass());
            }
        }
        for (MsgCreateReaction react : MsgCreateReaction.all()) {
            if (log.isDebugEnabled()) {
                log.debug(react.getClass());
            }
        }
        for (Scenario scene : Scenario.all()) {
            if (log.isDebugEnabled()) {
                log.debug(scene.getClass());
            }
        }

        new DiscordApiBuilder()
                .setToken(settingsController.getApiKey())
                .setAllIntents()
                .setWaitForUsersOnStartup(true)
                .setWaitForServersOnStartup(true)
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

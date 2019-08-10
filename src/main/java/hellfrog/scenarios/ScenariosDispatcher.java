package hellfrog.scenarios;

import hellfrog.common.CommonUtils;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

// TODO: после успешной реализации перенести всю логику в состав EventListener
public class ScenariosDispatcher
        implements MessageCreateListener,
        ReactionAddListener, ReactionRemoveListener {

    @Override
    public void onMessageCreate(@NotNull MessageCreateEvent event) {
        if (true) {
            return;
        }
        String strMessage = event.getMessageContent();
        Optional<Server> mayBeServer = event.getServer();

        if (mayBeServer.isPresent()) {
            Server server = mayBeServer.get();
            String serverBotPrefixNoSep = SettingsController
                    .getInstance()
                    .getBotPrefix(server.getId());
            if (strMessage.startsWith(serverBotPrefixNoSep)) {
                parseCommand(event);
            }
        } else {
            String globalBotPrefixNoSep = SettingsController
                    .getInstance()
                    .getGlobalCommonPrefix();
            if (strMessage.startsWith(globalBotPrefixNoSep)) {
                parseCommand(event);
            }
        }
    }

    private void parseCommand(@NotNull MessageCreateEvent event) {
        String rawMessageString = event.getMessageContent().strip();
        if (CommonUtils.isTrStringEmpty(rawMessageString)) return;
        Scenario.getALL().stream()
                .filter(scenario -> scenario.canExecute(rawMessageString))
                .findFirst()
                .ifPresent(scenario -> checkAndRunScenario(event, scenario));
    }

    private void checkAndRunScenario(@NotNull MessageCreateEvent event, @NotNull Scenario scenario) {

        if (event.isServerMessage()
                && event.getServer().isPresent()
                && event.getServerTextChannel().isPresent()
                && !scenario.canExecuteServerCommand(event, event.getServer().get(), event.getServerTextChannel().get().getId())) {

            scenario.showAccessDeniedServerMessage(event);
            return;
        }

        if (scenario.isOnlyServerCommand() && !event.isServerMessage()) {
            scenario.showErrorMessage("This command can't be run into private channel", event);
            return;
        }
    }

    @Override
    public void onReactionAdd(@NotNull ReactionAddEvent event) {

    }

    @Override
    public void onReactionRemove(@NotNull ReactionRemoveEvent event) {

    }
}

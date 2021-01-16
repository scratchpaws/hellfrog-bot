package hellfrog.common;

import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public class BroadCast
        implements CommonConstants {

    private static final Logger log = LogManager.getLogger(BroadCast.class.getSimpleName());

    private static final Color INFO_COLOR = Color.GREEN;
    private static final Color WARN_COLOR = Color.YELLOW;
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color UNKNOWN_COLOR = Color.BLACK;

    private static void sendServiceMessage(@NotNull final LongEmbedMessage broadcastMessage) {
        Optional<DiscordApi> mayBeApi = Optional.ofNullable(SettingsController.getInstance().getDiscordApi());
        final long serviceChannelId = SettingsController.getInstance()
                .getMainDBController()
                .getCommonPreferencesDAO()
                .getBotServiceChannelId();
        mayBeApi.ifPresentOrElse(discordApi -> discordApi
                        .getTextChannelById(serviceChannelId)
                        .ifPresentOrElse(textChannel -> {
                            if (textChannel.canYouSee() && textChannel.canYouWrite()
                                    && textChannel.canYouReadMessageHistory()
                                    && textChannel.canYouEmbedLinks()) {
                                try {
                                    broadcastMessage.send(textChannel).get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
                                } catch (Exception throwable) {
                                    LongEmbedMessage errMessage = new LongEmbedMessage()
                                            .setErrorStyle()
                                            .appendf("Unable to send message to channel %d: %s",
                                                    serviceChannelId, throwable.getMessage());
                                    log.error(errMessage.toString(), throwable);
                                    sendMessageToAllOwners(broadcastMessage);
                                    sendMessageToAllOwners(errMessage);
                                }
                            } else {
                                LongEmbedMessage errMessage = new LongEmbedMessage()
                                        .setErrorStyle()
                                        .appendf("There are no rights to send messages to the channel %d", serviceChannelId);
                                sendMessageToAllOwners(broadcastMessage);
                                sendMessageToAllOwners(errMessage);
                            }
                        }, () -> {
                            LongEmbedMessage errMessage = new LongEmbedMessage()
                                    .setErrorStyle()
                                    .appendf("Unable to resolve text channel %d", serviceChannelId);
                            sendMessageToAllOwners(broadcastMessage);
                            sendMessageToAllOwners(errMessage);
                        })
                , () -> log.fatal("Discord API is null. Unable to send service message \"{}\"",
                        broadcastMessage)
        );
    }

    private static void sendMessageToAllOwners(@NotNull final LongEmbedMessage broadcastMessage) {
        SettingsController settingsController = SettingsController.getInstance();
        List<Long> toNotify = new ArrayList<>();
        DiscordApi discordApi = settingsController.getDiscordApi();
        long mainOwner = discordApi.getOwnerId();
        toNotify.add(mainOwner);
        settingsController.getGlobalBotOwners().stream()
                .filter(ownerId -> (ownerId != mainOwner))
                .forEachOrdered(toNotify::add);

        toNotify.forEach(ownerId -> {
            try {
                User user = discordApi.getUserById(ownerId).get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
                broadcastMessage.send(user).get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
            } catch (Exception err) {
                log.fatal("Unable to send broadcast message \"" + broadcastMessage +
                        "\": " + err.getMessage(), err);
            }
        });
    }

    public static MessagesLogger getLogger() {
        return new MessagesLogger();
    }

    public static class MessagesLogger {

        private MessagesLogger() {
        }

        private final LongEmbedMessage resultMessage = new LongEmbedMessage();

        private static final int SEVERITY_INFO = 0;
        private static final int SEVERITY_WARN = 1;
        private static final int SEVERITY_ERROR = 2;

        private int severity = SEVERITY_INFO;

        private void addMessage(@NotNull String logLevel, @NotNull CharSequence message) {
            Calendar currentDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            String msg = String.format("[%tF %<tT] <%s> <%s>\n", currentDate, logLevel, message);
            resultMessage.append(msg);
        }

        public MessagesLogger addInfoMessage(@Nullable CharSequence message) {
            if (CommonUtils.isTrStringNotEmpty(message)) {
                addMessage("info", message);
                severity = Math.max(severity, SEVERITY_INFO);
            }
            return this;
        }

        public MessagesLogger addWarnMessage(@Nullable CharSequence message) {
            if (CommonUtils.isTrStringNotEmpty(message)) {
                addMessage("warn", message);
                severity = Math.max(severity, SEVERITY_WARN);
            }
            return this;
        }

        public MessagesLogger addErrorMessage(@Nullable CharSequence message) {
            if (CommonUtils.isTrStringNotEmpty(message)) {
                addMessage("error", message);
                severity = Math.max(severity, SEVERITY_ERROR);
            }
            return this;
        }

        public MessagesLogger addUnsafeUsageCE(@Nullable CharSequence message, @Nullable MessageCreateEvent event) {
            if (CommonUtils.isTrStringEmpty(message) || event == null) {
                return this;
            }
            String userData = "User with id: " + event.getMessageAuthor().getId() +
                    (event.getMessageAuthor().asUser().map(user -> " (" + user.getDiscriminatedName() + ") ").orElse(" ")) +
                    message;
            addWarnMessage(userData);
            return this;
        }

        public MessagesLogger addUnsafeUsageCE(@Nullable CharSequence message, @Nullable SingleReactionEvent event) {
            if (CommonUtils.isTrStringEmpty(message) || event == null) {
                return this;
            }
            String userData = "User with id: " + event.getUserId() +
                    (event.getUser().map(user -> " (" + user.getDiscriminatedName() + ") ").orElse(" ")) +
                    message;
            addWarnMessage(userData);
            return this;
        }

        public void send() {
            Color messageColor = switch (severity) {
                case SEVERITY_INFO -> INFO_COLOR;
                case SEVERITY_WARN -> WARN_COLOR;
                case SEVERITY_ERROR -> ERROR_COLOR;
                default -> UNKNOWN_COLOR;
            };
            resultMessage.setColor(messageColor);
            BroadCast.sendServiceMessage(resultMessage);
        }

        public MessagesLogger add(@Nullable MessagesLogger another) {
            if (another != null) {
                resultMessage.append(another.resultMessage);
                this.severity = Math.max(this.severity, another.severity);
            }
            return this;
        }
    }
}

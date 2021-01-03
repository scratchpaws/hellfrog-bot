package hellfrog.common;

import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BroadCast
        implements CommonConstants {

    private static final Logger log = LogManager.getLogger(BroadCast.class.getSimpleName());

    private static final Color INFO_COLOR = Color.GREEN;
    private static final Color WARN_COLOR = Color.YELLOW;
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color UNKNOWN_COLOR = Color.BLACK;

    public static void sendServiceMessage(final String broadcastMessage, final Color messageColor) {
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
                                    new MessageBuilder()
                                            .setEmbed(new EmbedBuilder()
                                                    .setDescription(broadcastMessage)
                                                    .setColor(messageColor))
                                            .send(textChannel)
                                            .get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
                                } catch (Exception throwable) {
                                    String errMessage = "Unable to send message to channel "
                                            + serviceChannelId + ": " + throwable.getMessage();
                                    log.error(errMessage, throwable);
                                    sendMessageToAllOwners(broadcastMessage, messageColor);
                                    sendMessageToAllOwners(errMessage, ERROR_COLOR);
                                }
                            } else {
                                String errMessage = "There are no rights to send messages to the channel "
                                        + serviceChannelId;
                                sendMessageToAllOwners(broadcastMessage, messageColor);
                                sendMessageToAllOwners(errMessage, ERROR_COLOR);
                            }
                        }, () -> {
                            String errMessage = "Unable to resolve text channel " + serviceChannelId;
                            sendMessageToAllOwners(broadcastMessage, messageColor);
                            sendMessageToAllOwners(errMessage, ERROR_COLOR);
                        })
                , () -> log.fatal("Discord API is null. Unable to send service message \"{}\"",
                        broadcastMessage)
        );
    }

    private static void sendMessageToAllOwners(String broadcastMessage, Color messageColor) {
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
                User user = discordApi.getUserById(ownerId).join();
                new MessageBuilder()
                        .setEmbed(new EmbedBuilder()
                                .setColor(messageColor)
                                .setDescription(broadcastMessage)
                                .setTimestampToNow())
                        .send(user)
                        .get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
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

        private final StringBuilder messagesLine = new StringBuilder();

        private static final int SEVERITY_INFO = 0;
        private static final int SEVERITY_WARN = 1;
        private static final int SEVERITY_ERROR = 2;

        private int severity = SEVERITY_INFO;

        private void addMessage(@NotNull String logLevel, @NotNull CharSequence message) {
            Calendar currentDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            String msg = String.format("[%tF %<tT] <%s> <%s>\n", currentDate, logLevel, message);
            messagesLine.append(msg);
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
            List<String> messages = CommonUtils.splitEqually(messagesLine.toString(), 2000);
            Color messageColor = switch (severity) {
                case SEVERITY_INFO -> INFO_COLOR;
                case SEVERITY_WARN -> WARN_COLOR;
                case SEVERITY_ERROR -> ERROR_COLOR;
                default -> UNKNOWN_COLOR;
            };
            for (String line : messages) {
                BroadCast.sendServiceMessage(line, messageColor);
            }
        }

        public MessagesLogger add(@Nullable MessagesLogger another) {
            if (another != null) {
                messagesLine.append(another.messagesLine);
            }
            return this;
        }
    }
}

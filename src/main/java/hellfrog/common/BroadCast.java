package hellfrog.common;

import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BroadCast
        implements CommonConstants {

    private static final Logger log = LogManager.getLogger(BroadCast.class.getSimpleName());

    public static void sendServiceMessage(String broadcastMessage) {
        Optional<DiscordApi> mayBeApi = Optional.ofNullable(SettingsController.getInstance().getDiscordApi());
        mayBeApi.ifPresentOrElse(discordApi ->
                        discordApi.getTextChannelById(SERVICE_MESSAGES_CHANNEL).ifPresentOrElse(textChannel -> {
                            if (textChannel.canYouSee() && textChannel.canYouWrite()
                                    && textChannel.canYouReadMessageHistory()
                                    && textChannel.canYouEmbedLinks()) {
                                try {
                                    new MessageBuilder()
                                            .setEmbed(new EmbedBuilder()
                                                    .setTitle("WARNING")
                                                    .setDescription(broadcastMessage)
                                                    .setColor(Color.RED))
                                            .send(textChannel)
                                            .get(OP_WAITING_TIMEOUT, OP_TIME_UNIT);
                                } catch (Exception throwable) {
                                    String errMessage = "Unable to send message to channel "
                                            + SERVICE_MESSAGES_CHANNEL + ": " + throwable.getMessage();
                                    log.error(errMessage, throwable);
                                    sendMessageToAllOwners(broadcastMessage);
                                    sendMessageToAllOwners(errMessage);
                                }
                            } else {
                                String errMessage = "There are no rights to send messages to the channel "
                                        + SERVICE_MESSAGES_CHANNEL;
                                sendMessageToAllOwners(broadcastMessage);
                                sendMessageToAllOwners(errMessage);
                            }
                        }, () -> {
                            String errMessage = "Unable to resolve text channel " + SERVICE_MESSAGES_CHANNEL;
                            sendMessageToAllOwners(broadcastMessage);
                            sendMessageToAllOwners(errMessage);
                        })
                , () -> log.fatal("Discord API is null. Unable to send service message \"{}\"",
                        broadcastMessage)
        );
    }

    private static void sendMessageToAllOwners(String broadcastMessage) {
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
                                .setTitle("WARNING")
                                .setColor(Color.RED)
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

    public static void sendBroadcastUnsafeUsageCE(String broadcastMessage, @NotNull MessageCreateEvent event) {
        String userData = "User with id: " + event.getMessageAuthor().getId() +
                (event.getMessageAuthor().asUser().map(user -> " (" + user.getDiscriminatedName() + ") ").orElse(" ")) +
                broadcastMessage;
        BroadCast.sendServiceMessage(userData);
    }
}

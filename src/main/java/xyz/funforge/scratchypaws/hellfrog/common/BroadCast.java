package xyz.funforge.scratchypaws.hellfrog.common;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BroadCast {

    public static void sendBroadcastToAllBotOwners(String broadcastMessage) {
        SettingsController settingsController = SettingsController.getInstance();
        List<Long> toNotify = new ArrayList<>();
        List<Long> alreadyNotified = new ArrayList<>();
        DiscordApi discordApi = settingsController.getDiscordApi();
        long mainOwner = discordApi.getOwnerId();
        toNotify.add(mainOwner);
        settingsController.getGlobalBotOwners().stream()
                .filter((ownerId) -> (ownerId != mainOwner))
                .forEachOrdered(toNotify::add);

        toNotify.stream()
                .filter((ownerId) -> (!alreadyNotified.contains(ownerId)))
                .forEachOrdered((ownerId) -> {
                    try {
                        User user = discordApi.getUserById(ownerId).join();
                        new MessageBuilder()
                                .setEmbed(new EmbedBuilder()
                                        .setTitle("WARNING")
                                        .setColor(Color.RED)
                                        .setDescription(broadcastMessage)
                                        .setTimestampToNow())
                                .send(user);
                        alreadyNotified.add(ownerId);
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                });
    }

    public static void sendBroadcastUnsafeUsageCE(String broadcastMessage, MessageCreateEvent event) {
        String userData = "User with id: " + event.getMessageAuthor().getId() +
                (event.getMessageAuthor().asUser().map(user -> " (" + user.getDiscriminatedName() + ") ").orElse(" ")) +
                broadcastMessage;
        BroadCast.sendBroadcastToAllBotOwners(userData);
    }
}

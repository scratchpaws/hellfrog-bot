package hellfrog.core;

import hellfrog.settings.SettingsController;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SessionsCheckTask
        implements Runnable {

    private final ScheduledFuture<?> scheduled;

    public SessionsCheckTask() {
        scheduled = Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this, 5L, 5L, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        SessionState.all().forEach(sessionState -> {
            if (!sessionState.inTimeout()) {
                terminateSessionState(sessionState);
            }
        });
    }

    public static void terminateSessionState(@NotNull SessionState sessionState) {
        SessionState.all().remove(sessionState);
        Optional.ofNullable(SettingsController.getInstance().getDiscordApi()).ifPresent(api ->
                api.getUserById(sessionState.getUserId()).thenAccept(user ->
                        api.getTextChannelById(sessionState.getTextChannelId()).ifPresent(textChannel -> {
                            User yourSelf = api.getYourself();
                            new MessageBuilder()
                                    .setEmbed(new EmbedBuilder()
                                            .setTimestampToNow()
                                            .setAuthor(yourSelf)
                                            .setColor(Color.RED)
                                            .setDescription(user.getMentionTag() + ", your response time is up"))
                                    .send(textChannel);
                            textChannel.getMessageById(sessionState.getMessageId())
                                    .thenAccept(Message::removeAllReactions);
                        })
                ));
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

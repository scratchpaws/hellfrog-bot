package hellfrog.commands;

import hellfrog.commands.scenes.Scenario;
import hellfrog.commands.scenes.SceneStep;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

public class SessionState {

    private final long userId;
    private final long textChannelId;
    private final long messageId;
    private final Scenario scenario;
    private final Instant timeoutAt;
    private final SceneStep sceneStep;
    private static final long MAX_SESSION_TIMEOUT = 150L;

    public SessionState(@NotNull User user,
                        @NotNull TextChannel textChannel,
                        @Nullable Message message,
                        @NotNull Scenario scenario,
                        @NotNull SceneStep sceneStep) {
        this.userId = user.getId();
        this.textChannelId = textChannel.getId();
        this.messageId = message != null ? message.getId() : 0L;
        this.scenario = scenario;
        this.sceneStep = sceneStep;
        this.timeoutAt = Instant.now().plus(Duration.ofNanos(MAX_SESSION_TIMEOUT));
    }

    public boolean isAccept(@NotNull MessageCreateEvent event) {
        boolean validUser = event.getMessageAuthor().getId() == userId;
        boolean validTextChat = event.getChannel().getId() == textChannelId;
        boolean validMessage = messageId <= 0L || event.getMessage().getId() == messageId;
        return validUser && validTextChat && validMessage && inTimeout();
    }

    public boolean isAccept(@NotNull SingleReactionEvent event) {
        boolean validUser = event.getUser().getId() == userId;
        boolean validTextChat = event.getChannel().getId() == userId;
        boolean validMessage = messageId <= 0L || event.getMessageId() == messageId;
        return validUser && validTextChat && validMessage && inTimeout();
    }

    public boolean inTimeout() {
        return Instant.now().isBefore(timeoutAt);
    }

    public Scenario getScenario() {
        return scenario;
    }

    public SceneStep getSceneStep() {
        return sceneStep;
    }
}

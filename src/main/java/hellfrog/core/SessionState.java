package hellfrog.core;

import hellfrog.commands.scenes.Scenario;
import hellfrog.commands.scenes.ScenarioState;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SessionState {

    private final long userId;
    private final long textChannelId;
    private final long messageId;
    private final boolean removeReaction;
    private final Scenario scenario;
    private final Instant timeoutAt;
    private final ScenarioState scenarioState;
    private static final long MAX_SESSION_TIMEOUT = 150L;

    private static final ConcurrentLinkedQueue<SessionState> ALL_STATES = new ConcurrentLinkedQueue<>();

    @Contract(pure = true)
    public static Queue<SessionState> all() {
        return ALL_STATES;
    }

    SessionState(@NotNull User user,
                        @NotNull TextChannel textChannel,
                        @Nullable Message message,
                        boolean removeReaction,
                        @NotNull Scenario scenario,
                        @NotNull ScenarioState scenarioState) {
        this.userId = user.getId();
        this.textChannelId = textChannel.getId();
        this.messageId = message != null ? message.getId() : 0L;
        this.removeReaction = removeReaction;
        this.scenario = scenario;
        this.scenarioState = scenarioState;
        this.timeoutAt = Instant.now().plus(Duration.ofSeconds(MAX_SESSION_TIMEOUT));
    }

    SessionState(long userId,
                 long textChannelId,
                 @Nullable Message message,
                 boolean removeReaction,
                 @NotNull Scenario scenario,
                 @NotNull ScenarioState scenarioState) {
        this.userId = userId;
        this.textChannelId = textChannelId;
        this.messageId = message != null ? message.getId() : 0L;
        this.removeReaction = removeReaction;
        this.scenario = scenario;
        this.scenarioState = scenarioState;
        this.timeoutAt = Instant.now().plus(Duration.ofSeconds(MAX_SESSION_TIMEOUT));
    }

    public boolean isAccept(@NotNull MessageCreateEvent event) {
        boolean validUser = event.getMessageAuthor().getId() == userId;
        boolean validTextChat = event.getChannel().getId() == textChannelId;
        return validUser && validTextChat && inTimeout();
    }

    public boolean isAccept(@NotNull SingleReactionEvent event) {
        boolean validUser = event.getUser().getId() == userId;
        boolean validTextChat = event.getChannel().getId() == textChannelId;
        boolean validMessage = messageId <= 0L || event.getMessageId() == messageId;
        return validUser && validTextChat && validMessage && inTimeout();
    }

    public boolean inTimeout() {
        return Instant.now().isBefore(timeoutAt);
    }

    public Scenario getScenario() {
        return scenario;
    }

    public ScenarioState getScenarioState() {
        return scenarioState;
    }

    public long getTextChannelId() {
        return textChannelId;
    }

    public long getUserId() {
        return userId;
    }

    public boolean isRemoveReaction() {
        return removeReaction;
    }

    public static SessionStateBuilder forScenario(Scenario scenario) {
        return new SessionStateBuilder()
                .setScenario(scenario);
    }

    public void putStateObject(String key, Object value) {
        scenarioState.put(key, value);
    }

    public <T> T getStateObject(String key, Class<T> type) {
        return scenarioState.get(key, type);
    }

    public long getMessageId() {
        return messageId;
    }

    public ClonedBuilder toBuilder() {
        return new ClonedBuilder(userId, textChannelId, scenario, scenarioState)
                .setRemoveReaction(removeReaction);
    }

    public static class ClonedBuilder {
        private final long userId;
        private final long textChannelId;
        private Message message = null;
        private boolean removeReaction = false;
        private final Scenario scenario;
        private final ScenarioState scenarioState;

        ClonedBuilder(long userId,
                      long textChannelId,
                      Scenario scenario,
                      ScenarioState scenarioState) {
            this.userId = userId;
            this.textChannelId = textChannelId;
            this.scenario = scenario;
            this.scenarioState = scenarioState;
        }

        public Message getMessage() {
            return message;
        }

        public ClonedBuilder setMessage(Message message) {
            this.message = message;
            return this;
        }

        public boolean isRemoveReaction() {
            return removeReaction;
        }

        public ClonedBuilder setRemoveReaction(boolean removeReaction) {
            this.removeReaction = removeReaction;
            return this;
        }

        public SessionState build() {
            return new SessionState(userId, textChannelId, message, removeReaction, scenario, scenarioState);
        }
    }

    public static class SessionStateBuilder {

        @Contract(pure = true)
        SessionStateBuilder() {}

        private User user = null;
        private TextChannel textChannel = null;
        private Message message = null;
        private boolean removeReaction = false;
        private Scenario scenario = null;
        private ScenarioState scenarioState = null;

        public User getUser() {
            return user;
        }

        public SessionStateBuilder setUser(User user) {
            this.user = user;
            return this;
        }

        public TextChannel getTextChannel() {
            return textChannel;
        }

        public SessionStateBuilder setTextChannel(TextChannel textChannel) {
            this.textChannel = textChannel;
            return this;
        }

        public Message getMessage() {
            return message;
        }

        public SessionStateBuilder setMessage(Message message) {
            this.message = message;
            return this;
        }

        public boolean isRemoveReaction() {
            return removeReaction;
        }

        public SessionStateBuilder setRemoveReaction(boolean removeReaction) {
            this.removeReaction = removeReaction;
            return this;
        }

        public Scenario getScenario() {
            return scenario;
        }

        public SessionStateBuilder setScenario(Scenario scenario) {
            this.scenario = scenario;
            return this;
        }

        public ScenarioState getScenarioState() {
            return scenarioState;
        }

        public SessionStateBuilder setScenarioState(ScenarioState scenarioState) {
            this.scenarioState = scenarioState;
            return this;
        }

        public SessionState build() {
            Objects.requireNonNull(user, "User cannot be null");
            Objects.requireNonNull(textChannel, "Text channel cannot be null");
            Objects.requireNonNull(scenario, "Scenario cannot be null");
            Objects.requireNonNull(scenarioState, "Scenario state cannot be null");
            return new SessionState(user, textChannel, message, removeReaction, scenario, scenarioState);
        }
    }
}

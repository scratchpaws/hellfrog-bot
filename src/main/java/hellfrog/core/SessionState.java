package hellfrog.core;

import hellfrog.commands.scenes.Scenario;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SessionState {

    private static final Logger log = LogManager.getLogger(SessionState.class.getSimpleName());
    private final long userId;
    private final long textChannelId;
    private final long messageId;
    private final boolean removeReaction;
    private final Scenario scenario;
    private final Instant timeoutAt;
    private final long stepId;

    private static final long MAX_SESSION_TIMEOUT = 150L;

    private static final ConcurrentLinkedQueue<SessionState> ALL_STATES = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Object> objectsMap;

    @Contract(pure = true)
    public static Queue<SessionState> all() {
        return ALL_STATES;
    }

    private SessionState(@NotNull final Scenario scenario,
                         final long stepId,
                         final long userId,
                         final long textChannelId,
                         final long messageId,
                         final boolean removeReaction,
                         @NotNull final ConcurrentHashMap<String, Object> objectsMap
    ) {
        this.scenario = scenario;
        this.stepId = stepId;
        this.userId = userId;
        this.textChannelId = textChannelId;
        this.messageId = messageId;
        this.removeReaction = removeReaction;
        this.objectsMap = objectsMap;
        this.timeoutAt = Instant.now().plus(Duration.ofSeconds(MAX_SESSION_TIMEOUT));
    }

    public boolean isAccept(@NotNull MessageCreateEvent event) {
        boolean validUser = event.getMessageAuthor().getId() == userId;
        boolean validTextChat = event.getChannel().getId() == textChannelId;
        return validUser && validTextChat && inTimeout();
    }

    public boolean isAccept(@NotNull SingleReactionEvent event) {
        boolean validUser = event.getUser().isPresent() && event.getUserId() == userId;
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

    public long getTextChannelId() {
        return textChannelId;
    }

    public long getUserId() {
        return userId;
    }

    public boolean isRemoveReaction() {
        return removeReaction;
    }

    public long getMessageId() {
        return messageId;
    }

    public static Builder forScenario(@NotNull final Scenario scenario, final long stepId) {
        return new Builder()
                .setStepId(stepId)
                .setScenario(scenario);
    }

    public long getStepId() {
        return stepId;
    }

    public void putValue(String key, Object value) {
        objectsMap.put(key, value);
    }

    public <T> T getValue(String key, Class<T> type) {
        Object obj = objectsMap.get(key);
        if (obj == null) return null;
        if (type.isInstance(obj)) {
            try {
                return type.cast(obj);
            } catch (ClassCastException err) {
                String errMsg = String.format("Unable case object with key %s to %s, step id %d: %s",
                        key, type.getName(), stepId, err.getMessage());
                log.error(errMsg, err);
                LogsStorage.addErrorMessage(errMsg);
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean stepIdIs(long that) {
        return stepId == that;
    }

    public boolean stepIdIs(long that1, long that2) {
        return stepId == that1
                || stepId == that2;
    }

    public Builder toBuilder() {
        return new Builder(objectsMap)
                .setStepId(stepId)
                .setUserId(userId)
                .setTextChannelId(textChannelId)
                .setMessageId(messageId)
                .setRemoveReaction(removeReaction)
                .setScenario(scenario);
    }

    public Builder toBuilderWithStepId(long newStepId) {
        return new Builder(objectsMap)
                .setStepId(newStepId)
                .setUserId(userId)
                .setTextChannelId(textChannelId)
                .setMessageId(messageId)
                .setRemoveReaction(removeReaction)
                .setScenario(scenario);
    }

    public SessionState resetTimeout() {
        return new SessionState(scenario, stepId, userId, textChannelId, messageId, removeReaction, objectsMap);
    }

    public static class Builder {

        private long stepId = 0L;
        private long userId = 0L;
        private long textChannelId = 0L;
        private long messageId = 0L;
        private boolean removeReaction = true;
        private Scenario scenario = null;
        private final ConcurrentHashMap<String, Object> objectsMap;

        @Contract(pure = true)
        private Builder() {
            this.objectsMap = new ConcurrentHashMap<>();
        }

        @Contract(pure = true)
        private Builder(@NotNull final ConcurrentHashMap<String, Object> objectsMap) {
            this.objectsMap = objectsMap;
        }

        public long getStepId() {
            return stepId;
        }

        public Builder setStepId(long stepId) {
            this.stepId = stepId;
            return this;
        }

        public long getUserId() {
            return userId;
        }

        Builder setUserId(long userId) {
            this.userId = userId;
            return this;
        }

        public boolean isRemoveReaction() {
            return removeReaction;
        }

        public Scenario getScenario() {
            return scenario;
        }

        public long getTextChannelId() {
            return textChannelId;
        }

        Builder setTextChannelId(long textChannelId) {
            this.textChannelId = textChannelId;
            return this;
        }

        public long getMessageId() {
            return messageId;
        }

        Builder setMessageId(long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder setUser(@Nullable final User user) {
            this.userId = user != null ? user.getId() : 0L;
            return this;
        }

        public Builder setTextChannel(final @Nullable TextChannel textChannel) {
            this.textChannelId = textChannel != null ? textChannel.getId() : 0L;
            return this;
        }

        public Builder setMessage(final @Nullable Message message) {
            this.messageId = message != null ? message.getId() : 0L;
            return this;
        }

        public Builder setRemoveReaction(boolean removeReaction) {
            this.removeReaction = removeReaction;
            return this;
        }

        Builder setScenario(final @NotNull Scenario scenario) {
            this.scenario = scenario;
            return this;
        }

        public Builder putValue(String key, Object value) {
            objectsMap.put(key, value);
            return this;
        }

        public <T> T getValue(String key, Class<T> type) {
            Object obj = objectsMap.get(key);
            if (obj == null) return null;
            if (type.isInstance(obj)) {
                try {
                    return type.cast(obj);
                } catch (ClassCastException err) {
                    String errMsg = String.format("Unable case object with key %s to %s, step id %d: %s",
                            key, type.getName(), stepId, err.getMessage());
                    log.error(errMsg, err);
                    LogsStorage.addErrorMessage(errMsg);
                    return null;
                }
            } else {
                return null;
            }
        }

        public SessionState build() {
            if (userId == 0L || textChannelId == 0L || scenario == null) {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                if (stackTrace.length >= 2) {
                    String errMsg = String.format("BUG: detected %s::build() usage from %s (line %d) without " +
                                    "required items: userId == %d (required non zero), textChannelId == %d (required non zero)," +
                                    "scenario == %s (required non null)",
                            this.getClass().getName(), stackTrace[1].getClassName(), stackTrace[1].getLineNumber(),
                            userId, textChannelId, (scenario == null ? null : scenario.getClass().getSimpleName()));
                    LogsStorage.addErrorMessage(errMsg);
                }
            }
            if (userId == 0L) {
                throw new IllegalArgumentException("User cannot be empty");
            }
            if (textChannelId == 0L)
                throw new IllegalArgumentException("Text channel cannot be empty");
            Objects.requireNonNull(scenario, "Scenario cannot be null");
            return new SessionState(scenario, stepId, userId, textChannelId, messageId, removeReaction, objectsMap);
        }
    }
}

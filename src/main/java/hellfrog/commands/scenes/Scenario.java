package hellfrog.commands.scenes;

import hellfrog.commands.ACLCommand;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class Scenario
        extends ACLCommand
        implements CommonConstants {

    private static final List<Scenario> ALL = CodeSourceUtils.childClassInstancesCollector(Scenario.class);
    private static final boolean RUN_SCENARIO_RESULT = true;

    public Scenario(@NotNull String prefix, @NotNull String description) {
        super(prefix, description);
    }

    @Contract(pure = true)
    public static List<Scenario> all() {
        return ALL;
    }

    public boolean canExecute(String rawCommand) {
        return !CommonUtils.isTrStringEmpty(rawCommand)
                && rawCommand.strip().equalsIgnoreCase(super.getPrefix());
    }

    public final void firstRun(@NotNull MessageCreateEvent event) {

        super.updateLastUsage();

        boolean isBotOwner = canExecuteGlobalCommand(event);

        event.getMessageAuthor().asUser().ifPresent(user -> {
            event.getServerTextChannel().ifPresent(serverTextChannel -> {
                if (!canExecuteServerCommand(event, serverTextChannel.getServer())) {
                    showAccessDeniedServerMessage(event);
                    return;
                }
                executeServerFirstRun(event, serverTextChannel.getServer(), serverTextChannel, user, isBotOwner);
            });
            event.getPrivateChannel().ifPresent(privateChannel -> {
                if (isOnlyServerCommand()) {
                    showErrorMessage("This command can't be run into private channel", event);
                    return;
                }
                executePrivateFirstRun(event, privateChannel, user, isBotOwner);
            });
        });
    }

    protected abstract void executePrivateFirstRun(@NotNull MessageCreateEvent event,
                                                   @NotNull PrivateChannel privateChannel,
                                                   @NotNull User user,
                                                   boolean isBotOwner);

    protected abstract void executeServerFirstRun(@NotNull MessageCreateEvent event,
                                                  @NotNull Server server,
                                                  @NotNull ServerTextChannel serverTextChannel,
                                                  @NotNull User user,
                                                  boolean isBotOwner);

    /**
     * Последующее выполнение сценария. Вызывается при поступлении сообщения в чате
     *
     * @param event        событие нового сообщения
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     */
    public final void executeMessageStep(@NotNull MessageCreateEvent event,
                                         @NotNull SessionState sessionState) {
        super.updateLastUsage();

        boolean doRollback = true;
        boolean isBotOwner = canExecuteGlobalCommand(event);

        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        if (mayBeUser.isPresent()) {
            User user = mayBeUser.get();
            Optional<ServerTextChannel> mayBeServerChannel = event.getServerTextChannel();
            if (mayBeServerChannel.isPresent()) {
                ServerTextChannel serverTextChannel = mayBeServerChannel.get();
                Server server = serverTextChannel.getServer();
                if (!canExecuteServerCommand(event, server)) {
                    showAccessDeniedServerMessage(event);
                    doRollback = false;
                } else {
                    if (serverMessageStep(event, server, serverTextChannel, user, sessionState, isBotOwner)) {
                        doRollback = false;
                    }
                }
            }
            Optional<PrivateChannel> mayBePrivateChannel = event.getPrivateChannel();
            if (mayBePrivateChannel.isPresent()) {
                PrivateChannel privateChannel = mayBePrivateChannel.get();
                if (privateMessageStep(event, privateChannel, user, sessionState, isBotOwner)) {
                    doRollback = false;
                }
            }
        }

        if (doRollback) {
            rollbackState(sessionState);
        }
    }

    protected abstract boolean privateMessageStep(@NotNull MessageCreateEvent event,
                                                  @NotNull PrivateChannel privateChannel,
                                                  @NotNull User user,
                                                  @NotNull SessionState sessionState,
                                                  boolean isBotOwner);

    protected abstract boolean serverMessageStep(@NotNull MessageCreateEvent event,
                                                 @NotNull Server server,
                                                 @NotNull ServerTextChannel serverTextChannel,
                                                 @NotNull User user,
                                                 @NotNull SessionState sessionState,
                                                 boolean isBotOwner);

    /**
     * Последующее выполнение сценария. Вызывается при добалении либо удалении эмодзи в текстовом чате
     * на сообщении, созданном в сценарии ранее
     *
     * @param event        событие реакции (добавление/удаление)
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     */
    public final void executeReactionStep(@NotNull SingleReactionEvent event,
                                          @NotNull SessionState sessionState) {
        super.updateLastUsage();
        boolean isAddReaction = event instanceof ReactionAddEvent;
        boolean isBotOwner = canExecuteGlobalCommand(event);
        User user = event.getUser();
        boolean doRollback = true;

        Optional<ServerTextChannel> mayBeServerChannel = event.getServerTextChannel();
        if (mayBeServerChannel.isPresent()) {
            ServerTextChannel serverTextChannel = mayBeServerChannel.get();
            Server server = serverTextChannel.getServer();
            if (!canExecuteServerCommand(event, server)) {
                showAccessDeniedServerMessage(event);
                if (isAddReaction) {
                    ((ReactionAddEvent)event).removeReaction();
                }
                doRollback = false;
            } else {
                if (serverReactionStep(isAddReaction, event, server, serverTextChannel,
                        user, sessionState, isBotOwner)) {
                    doRollback = false;
                }
            }
        }
        Optional<PrivateChannel> mayBePrivateChannel = event.getPrivateChannel();
        if (mayBePrivateChannel.isPresent()) {
            PrivateChannel privateChannel = mayBePrivateChannel.get();
            if (privateReactionStep(isAddReaction, event, privateChannel, user, sessionState, isBotOwner)) {
                doRollback = false;
            }
        }

        if (doRollback) {
            rollbackState(sessionState);
            if (isAddReaction) {
                ((ReactionAddEvent)event).removeReaction();
            }
        }
    }

    protected abstract boolean privateReactionStep(boolean isAddReaction,
                                                   @NotNull SingleReactionEvent event,
                                                   @NotNull PrivateChannel privateChannel,
                                                   @NotNull User user,
                                                   @NotNull SessionState sessionState,
                                                   boolean isBotOwner);

    protected abstract boolean serverReactionStep(boolean isAddReaction,
                                                  @NotNull SingleReactionEvent event,
                                                  @NotNull Server server,
                                                  @NotNull ServerTextChannel serverTextChannel,
                                                  @NotNull User user,
                                                  @NotNull SessionState sessionState,
                                                  boolean isBotOwner);

    /**
     * Отправка Embed внутри нового сообщения, с ожиданием получения сообщения каналом.
     *
     * @param embedBuilder подготовленный embed
     * @param target       целевой канал для отправки сообщения
     * @return отправленное сообщение (либо не отправленное)
     */
    protected Optional<Message> displayMessage(@Nullable EmbedBuilder embedBuilder,
                                               @Nullable Messageable target) {
        if (embedBuilder == null || target == null) {
            return Optional.empty();
        }

        embedBuilder.setTimestampToNow();
        Optional.ofNullable(SettingsController.getInstance().getDiscordApi()).ifPresent(api ->
                embedBuilder.setAuthor(api.getYourself()));
        embedBuilder.setColor(Color.green);

        try {
            return Optional.ofNullable(new MessageBuilder()
                    .setEmbed(embedBuilder)
                    .send(target)
                    .get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (Exception err) {
            log.error("Unable to send message: " + err.getMessage(), err);
            return Optional.empty();
        }
    }

    /**
     * Добавление реакции сообщению с ожиданием проставления реакций сообщению
     *
     * @param message         сообщение, которому необходимо добавить реакции
     * @param resolvedEmojies кастомные эмодзи для реакций
     * @param unicodeEmojies  unicode-эмодзи для реакций
     * @return успех проставления всех реакций
     */
    protected boolean addReactions(@NotNull Message message,
                                   @Nullable List<KnownCustomEmoji> resolvedEmojies,
                                   @Nullable List<String> unicodeEmojies) {

        boolean existsResolvedEmoji = resolvedEmojies != null && !resolvedEmojies.isEmpty();
        boolean existsUnicodeEmoji = unicodeEmojies != null && !unicodeEmojies.isEmpty();
        if (!existsResolvedEmoji && !existsUnicodeEmoji) {
            return false;
        }

        boolean success = true;
        if (existsResolvedEmoji) {
            for (KnownCustomEmoji knownCustomEmoji : resolvedEmojies) {
                try {
                    message.addReaction(knownCustomEmoji).get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception err) {
                    log.error("Unable to add reaction: " + err.getMessage(), err);
                    success = false;
                }
            }
        }
        if (existsUnicodeEmoji) {
            for (String unicodeEmoji : unicodeEmojies) {
                try {
                    message.addReaction(unicodeEmoji).get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception err) {
                    log.error("Unable to add reaction: " + err.getMessage(), err);
                    success = false;
                }
            }
        }
        return success;
    }

    protected boolean dropPreviousStateEmoji(@NotNull SessionState sessionState) {
        DiscordApi api = SettingsController.getInstance().getDiscordApi();
        if (api == null) return false;

        Optional<TextChannel> mayBeChannel = api.getTextChannelById(sessionState.getTextChannelId());
        if (mayBeChannel.isPresent()) {
            TextChannel textChannel = mayBeChannel.get();
            try {
                Message msg = textChannel.getMessageById(sessionState.getMessageId())
                        .get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                try {
                    msg.removeAllReactions().get(OP_WAITING_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (Exception delErr) {
                    log.error("Unable to delete all reactions by message: channel id: "
                            + sessionState.getTextChannelId() + ", message id: "
                            + sessionState.getMessageId(), delErr);
                }
                return true;
            } catch (Exception err) {
                log.error("Unable to fetch history message: channel id: "
                        + sessionState.getTextChannelId() + ", message id: "
                        + sessionState.getMessageId(), err);
                return false;
            }
        }

        return false;
    }

    protected void commitState(@NotNull SessionState sessionState) {
        SessionState.all().add(sessionState);
    }

    protected void rollbackState(@NotNull SessionState sessionState) {
        SessionState.all().add(sessionState.resetTimeout());
    }

    protected boolean equalsUnicodeReaction(@NotNull SingleReactionEvent event,
                                            @NotNull String unicodeEmoji) {
        return event.getEmoji().asUnicodeEmoji()
                .map(unicodeEmoji::equals)
                .orElse(false);
    }

    protected boolean equalsUnicodeReactions(@NotNull SingleReactionEvent event,
                                             @NotNull String firstEmoji, @NotNull String secondEmoji) {
        return this.equalsUnicodeReaction(event, firstEmoji)
                || this.equalsUnicodeReaction(event, secondEmoji);
    }
}

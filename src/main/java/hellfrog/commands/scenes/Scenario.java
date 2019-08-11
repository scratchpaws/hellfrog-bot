package hellfrog.commands.scenes;

import hellfrog.commands.ACLCommand;
import hellfrog.common.CodeSourceUtils;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
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

    public boolean canExecute(String rawCommand) {
        return !CommonUtils.isTrStringEmpty(rawCommand)
                && rawCommand.strip().equalsIgnoreCase(super.getPrefix());
    }

    @Contract(pure = true)
    public static List<Scenario> all() {
        return ALL;
    }

    public boolean firstRun(@NotNull MessageCreateEvent event) {

        super.updateLastUsage();

        boolean canAccess = event.getServer()
                .map(server -> canExecuteServerCommand(event, server))
                .orElse(true);
        if (!canAccess) {
            showAccessDeniedServerMessage(event);
            return RUN_SCENARIO_RESULT;
        }

        if (isOnlyServerCommand() && !event.isServerMessage()) {
            showErrorMessage("This command can't be run into private channel", event);
            return RUN_SCENARIO_RESULT;
        }

        executeFirstRun(event);
        return RUN_SCENARIO_RESULT;
    }

    /**
     * Инициализация выполнения сценария. Вызывается при вводе соответствующей команды,
     * соответствующей префиксу сценария
     *
     * @param event событие нового сообщения
     */
    protected abstract void executeFirstRun(@NotNull MessageCreateEvent event);

    /**
     * Последующее выполнение сценария. Вызывается при поступлении сообщения в чате
     *
     * @param event        событие нового сообщения
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     */
    public abstract void executeMessageStep(@NotNull MessageCreateEvent event,
                                            @NotNull SessionState sessionState);

    /**
     * Последующее выполнение сценария. Вызывается при добалении либо удалении эмодзи в текстовом чате
     * на сообщении, созданном в сценарии ранее
     *
     * @param event        событие реакции (добавление/удаление)
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     */
    public abstract void executeReactionStep(@NotNull SingleReactionEvent event,
                                             @NotNull SessionState sessionState);

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

    public void commitState(@NotNull SessionState sessionState) {
        SessionState.all().add(sessionState);
    }

    protected boolean equalsUnicodeReaction(@NotNull SingleReactionEvent event,
                                            @NotNull String unicodeEmoji) {
        return event.getEmoji().asUnicodeEmoji()
                .map(unicodeEmoji::equals)
                .orElse(false);
    }
}

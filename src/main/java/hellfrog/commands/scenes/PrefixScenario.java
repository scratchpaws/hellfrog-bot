package hellfrog.commands.scenes;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.CommonUtils;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PrefixScenario
        extends Scenario {

    private static final String PREFIX = "prefix";
    private static final String DESCRIPTION = "Change the bot commands prefix";
    private static final String EMOJI_CHANGE_SERVER = EmojiParser.parseToUnicode(":arrow_forward:");
    private static final String EMOJI_CHANGE_GLOBAL = EMOJI_CHANGE_SERVER;
    private static final String EMOJI_CLOSE = EmojiParser.parseToUnicode(":x:");
    private static final String EMOJI_ONE_SELECT_SERVER = EmojiParser.parseToUnicode(":one:");
    private static final String EMOJI_TWO_SELECT_GLOBAL = EmojiParser.parseToUnicode(":two:");
    private static final List<String> SERVER_CHANGE_INIT_EMOJI =
            List.of(EMOJI_CHANGE_SERVER, EMOJI_CLOSE);
    private static final List<String> GLOBAL_CHANGE_INIT_EMOJI =
            List.of(EMOJI_CHANGE_GLOBAL, EMOJI_CLOSE);
    private static final List<String> GLOBAL_OR_SERVER_INIT_EMOJI =
            List.of(EMOJI_ONE_SELECT_SERVER, EMOJI_TWO_SELECT_GLOBAL, EMOJI_CLOSE);
    private static final List<String> ONLY_CLOSE_EMOJI = List.of(EMOJI_CLOSE);

    private static final String WELCOME_MESSAGE = "The bot prefix defines the character set"
            + " that should precede all other bot commands.\n"
            + "For example, if a `>>` is set as the prefix, then the command `help` is called as follows:\n"
            + "`>> help`\n"
            + "The prefix is divided into global and server. "
            + "Global works in private messages and can only be changed "
            + "by the owner of the bot. Server-side runs in text chats of "
            + "the current server and can be changed either by a user with "
            + "administrator rights, or by a user who is explicitly allowed to execute this command.\n"
            + "The mention of the bot can also be a prefix.\n"
            + "Using the following reactions, specify the action "
            + "you want to perform on the bot prefix.\n";

    private static final long SERVER_PREFIX_CHANGE_INIT = 0L;
    private static final long GLOBAL_ONLY_PREFIX_CHANGE_INIT = 1L;
    private static final long GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT = 2L;
    private static final long SERVER_PREFIX_AWAIT_ENTER = 3L;
    private static final long GLOBAL_PREFIX_AWAIT_ENTER = 4L;

    public PrefixScenario() {
        super(PREFIX, DESCRIPTION);
    }

    /**
     * Инициализация выполнения сценария. Вызывается при вводе соответствующей команды,
     * соответствующей префиксу сценария
     *
     * @param event событие нового сообщения
     */
    @Override
    protected void executeFirstRun(@NotNull MessageCreateEvent event) {
        event.getMessageAuthor().asUser().ifPresent(user -> {
            SettingsController settingsController = SettingsController.getInstance();
            boolean isBotOwner = settingsController.isGlobalBotOwner(user.getId())
                    || event.getApi().getOwnerId() == user.getId();
            boolean isServerMessage = event.isServerMessage();

            if (isBotOwner && isServerMessage) {
                firstRunChangeGlobalOrServerPrefix(event, user);
            } else if (isServerMessage) {
                firstRunChangeServerPrefix(event, user);
            } else if (isBotOwner) {
                firstRunChangeGlobalServerPrefix(event, user);
            }
            if (!isServerMessage && !isBotOwner) {
                super.showErrorMessage("You cannot change the server bot command prefix in private", event);
            }
        });
    }

    private void firstRunChangeServerPrefix(@NotNull MessageCreateEvent event,
                                            @NotNull User user) {
        event.getServer().ifPresent(server -> {
            SettingsController settingsController = SettingsController.getInstance();
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Bot commands prefix editor")
                    .setDescription(WELCOME_MESSAGE
                            + "\\* " + EMOJI_CHANGE_SERVER + " - change current server bot prefix\n"
                            + "\\* " + EMOJI_CLOSE + " - exit editor")
                    .addField("Current server bot prefix", settingsController.getBotPrefix(server.getId()))
                    .addField("Current global bot prefix", settingsController.getGlobalCommonPrefix());
            super.displayMessage(embedBuilder, event.getChannel()).ifPresent(message -> {
                if (super.addReactions(message, null, SERVER_CHANGE_INIT_EMOJI)) {
                    ScenarioState scenarioState = new ScenarioState(SERVER_PREFIX_CHANGE_INIT);
                    SessionState sessionState = SessionState.forScenario(this)
                            .setMessage(message)
                            .setRemoveReaction(true)
                            .setTextChannel(event.getChannel())
                            .setUser(user)
                            .setScenarioState(scenarioState)
                            .build();
                    super.commitState(sessionState);
                }
            });
        });
    }

    private void firstRunChangeGlobalServerPrefix(@NotNull MessageCreateEvent event,
                                                  @NotNull User user) {
        SettingsController settingsController = SettingsController.getInstance();
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Bot commands prefix editor")
                .setDescription(WELCOME_MESSAGE
                        + "\\* " + EMOJI_CHANGE_GLOBAL + " - change current **global** bot prefix\n"
                        + "\\* " + EMOJI_CLOSE + " - exit editor")
                .addField("Current global bot prefix", settingsController.getGlobalCommonPrefix());
        super.displayMessage(embedBuilder, event.getChannel()).ifPresent(message -> {
            if (super.addReactions(message, null, GLOBAL_CHANGE_INIT_EMOJI)) {
                ScenarioState scenarioState = new ScenarioState(GLOBAL_ONLY_PREFIX_CHANGE_INIT);
                SessionState sessionState = SessionState.forScenario(this)
                        .setMessage(message)
                        .setRemoveReaction(true)
                        .setTextChannel(event.getChannel())
                        .setUser(user)
                        .setScenarioState(scenarioState)
                        .build();
                super.commitState(sessionState);
            }
        });
    }

    private void firstRunChangeGlobalOrServerPrefix(@NotNull MessageCreateEvent event,
                                                    @NotNull User user) {
        event.getServer().ifPresent(server -> {
            SettingsController settingsController = SettingsController.getInstance();
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Bot commands prefix editor")
                    .setDescription(WELCOME_MESSAGE
                            + "\\* " + EMOJI_ONE_SELECT_SERVER + " - change **current server** bot prefix\n"
                            + "\\* " + EMOJI_TWO_SELECT_GLOBAL + " - change **global** bot prefix\n"
                            + "\\* " + EMOJI_CLOSE + " - exit editor")
                    .addField("Current server bot prefix", settingsController.getBotPrefix(server.getId()))
                    .addField("Current global bot prefix", settingsController.getGlobalCommonPrefix());
            super.displayMessage(embedBuilder, event.getChannel()).ifPresent(message -> {
                if (super.addReactions(message, null, GLOBAL_OR_SERVER_INIT_EMOJI)) {
                    ScenarioState scenarioState = new ScenarioState(GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT);
                    SessionState sessionState = SessionState.forScenario(this)
                            .setMessage(message)
                            .setRemoveReaction(true)
                            .setTextChannel(event.getChannel())
                            .setUser(user)
                            .setScenarioState(scenarioState)
                            .build();
                    super.commitState(sessionState);
                }
            });
        });
    }

    /**
     * Последующее выполнение сценария. Вызывается при поступлении сообщения в чате
     *
     * @param event        событие нового сообщения
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     */
    @Override
    public void executeMessageStep(@NotNull MessageCreateEvent event, @NotNull SessionState sessionState) {
        long previousStepId = sessionState.getScenarioState().getStepId();
        event.getMessageAuthor().asUser().ifPresentOrElse(user -> {
            SettingsController settingsController = SettingsController.getInstance();
            boolean isBotOwner = user.isBotOwner() || settingsController.isGlobalBotOwner(user.getId());
            boolean changeServer = event.getServer()
                    .map(server -> super.canExecuteServerCommand(event, server, event.getChannel().getId()))
                    .orElse(false) && previousStepId == SERVER_PREFIX_AWAIT_ENTER;
            boolean changeGlobal = isBotOwner && previousStepId == GLOBAL_PREFIX_AWAIT_ENTER;
            if (changeGlobal || changeServer) {
                super.dropPreviousStateEmoji(sessionState);

                String newPrefix = event.getMessageContent().strip();
                String oldPrefix;
                if (changeGlobal) {
                    oldPrefix = settingsController.getGlobalCommonPrefix();
                } else {
                    oldPrefix = event.getServer().map(server ->
                            settingsController.getServerPreferences(server.getId()).getBotPrefix())
                            .orElse("");
                }
                if (oldPrefix.equals(newPrefix)) {
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle(changeGlobal ? "Change global prefix" : "Change server prefix")
                            .setDescription("Old and new prefix is same. Please try again.\n" +
                                    "Enter the new prefix with the new message. " +
                                    "It is preferable to use a prefix that will be " +
                                    "conveniently entered from the phone.");
                    displayMessage(embedBuilder, event.getChannel()).ifPresent(message ->
                        super.addReactions(message, null, ONLY_CLOSE_EMOJI));
                    super.commitState(sessionState);
                } else {
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setTitle("Bot commands prefix editor")
                            .setDescription((changeGlobal ? "Global" : "Server")
                                    + " bot prefix changed successfully")
                            .addField("New prefix:", newPrefix);
                    super.displayMessage(embedBuilder, event.getChannel());

                    if (changeGlobal) {
                        settingsController.setGlobalCommonPrefix(newPrefix);
                        settingsController.saveCommonPreferences();
                    } else {
                        event.getServer().ifPresent(server -> {
                            settingsController.getServerPreferences(server.getId())
                                    .setBotPrefix(newPrefix);
                            settingsController.saveServerSideParameters(server.getId());
                        });
                    }
                }
            } else {
                super.commitState(sessionState);
            }
        }, () -> super.commitState(sessionState));
    }

    /**
     * Последующее выполнение сценария. Вызывается при добалении либо удалении эмодзи в текстовом чате
     * на сообщении, созданном в сценарии ранее
     *
     * @param event        событие реакции (добавление/удаление)
     * @param sessionState состояние запущенного для пользователя сценария в текстовом чате
     */
    @Override
    public void executeReactionStep(@NotNull SingleReactionEvent event, @NotNull SessionState sessionState) {

        long previousStepId = sessionState.getScenarioState().getStepId();

        boolean isCloseReaction = super.equalsUnicodeReaction(event, EMOJI_CLOSE);
        boolean isChangeServerReaction =
                (super.equalsUnicodeReaction(event, EMOJI_CHANGE_SERVER)
                        || super.equalsUnicodeReaction(event, EMOJI_ONE_SELECT_SERVER))
                        && CommonUtils.in(previousStepId, SERVER_PREFIX_CHANGE_INIT, GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT);
        boolean isChangeGlobalReaction =
                (super.equalsUnicodeReaction(event, EMOJI_CHANGE_GLOBAL)
                        || super.equalsUnicodeReaction(event, EMOJI_TWO_SELECT_GLOBAL))
                        && CommonUtils.in(previousStepId, GLOBAL_ONLY_PREFIX_CHANGE_INIT, GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT);

        if (isCloseReaction || isChangeGlobalReaction || isChangeServerReaction) {
            super.dropPreviousStateEmoji(sessionState);

            if (isCloseReaction) {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle("Bot commands prefix editor")
                        .setDescription("Canceled");
                super.displayMessage(embedBuilder, event.getChannel());
            } else {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle(isChangeGlobalReaction ? "Change global prefix" : "Change server prefix")
                        .setDescription("Enter the new prefix with the new message. " +
                                "It is preferable to use a prefix that will be " +
                                "conveniently entered from the phone.");
                displayMessage(embedBuilder, event.getChannel()).ifPresentOrElse(message -> {
                    if (super.addReactions(message, null, ONLY_CLOSE_EMOJI)) {
                        SessionState newState = sessionState.toBuilder()
                                .setRemoveReaction(true)
                                .setMessage(message)
                                .changeScenarioStateId(isChangeGlobalReaction ? GLOBAL_PREFIX_AWAIT_ENTER : SERVER_PREFIX_AWAIT_ENTER)
                                .build();
                        super.commitState(newState);
                    } else {
                        super.commitState(sessionState);
                    }
                }, () -> super.commitState(sessionState));
            }
        } else {
            if (event instanceof ReactionAddEvent) {
                ((ReactionAddEvent) event).removeReaction();
            }
            super.commitState(sessionState);
        }
    }
}

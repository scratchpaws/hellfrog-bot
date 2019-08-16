package hellfrog.commands.scenes;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.core.SessionState;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
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
    private static final List<String> SERVER_ONLY_CHANGE_INIT_EMOJI =
            List.of(EMOJI_CHANGE_SERVER, EMOJI_CLOSE);
    private static final List<String> GLOBAL_ONLY_CHANGE_INIT_EMOJI =
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

    private static final long SERVER_ONLY_PREFIX_CHANGE_INIT = 0L;
    private static final long GLOBAL_ONLY_PREFIX_CHANGE_INIT = 1L;
    private static final long GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT = 2L;
    private static final long SERVER_PREFIX_AWAIT_ENTER = 3L;
    private static final long GLOBAL_PREFIX_AWAIT_ENTER = 4L;

    public PrefixScenario() {
        super(PREFIX, DESCRIPTION);
    }

    @Override
    protected void executePrivateFirstRun(@NotNull MessageCreateEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          boolean isBotOwner) {
        SettingsController settingsController = SettingsController.getInstance();
        if (isBotOwner) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Bot commands prefix editor")
                    .setDescription(WELCOME_MESSAGE
                            + "\\* " + EMOJI_CHANGE_GLOBAL + " - change current **global** bot prefix\n"
                            + "\\* " + EMOJI_CLOSE + " - exit editor")
                    .addField("Current global bot prefix", settingsController.getGlobalCommonPrefix());
            super.displayMessage(embedBuilder, event.getChannel()).ifPresent(message -> {
                if (super.addReactions(message, null, GLOBAL_ONLY_CHANGE_INIT_EMOJI)) {
                    SessionState sessionState = SessionState.forScenario(this, GLOBAL_ONLY_PREFIX_CHANGE_INIT)
                            .setMessage(message)
                            .setRemoveReaction(true)
                            .setTextChannel(event.getChannel())
                            .setUser(user)
                            .build();
                    super.commitState(sessionState);
                }
            });
        } else {
            super.showErrorMessage("You cannot change the server bot command prefix in private", event);
        }
    }

    @Override
    protected void executeServerFirstRun(@NotNull MessageCreateEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         boolean isBotOwner) {
        SettingsController settingsController = SettingsController.getInstance();
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Bot commands prefix editor")
                .addField("Current server bot prefix", settingsController.getBotPrefix(server.getId()))
                .addField("Current global bot prefix", settingsController.getGlobalCommonPrefix());
        if (isBotOwner) {
            embedBuilder.setDescription(WELCOME_MESSAGE
                    + "\\* " + EMOJI_ONE_SELECT_SERVER + " - change **current server** bot prefix\n"
                    + "\\* " + EMOJI_TWO_SELECT_GLOBAL + " - change **global** bot prefix\n"
                    + "\\* " + EMOJI_CLOSE + " - exit editor");
        } else {
            embedBuilder.setDescription(WELCOME_MESSAGE
                    + "\\* " + EMOJI_CHANGE_SERVER + " - change current server bot prefix\n"
                    + "\\* " + EMOJI_CLOSE + " - exit editor");
        }
        super.displayMessage(embedBuilder, event.getChannel()).ifPresent(message -> {
            List<String> emojiList = isBotOwner ? GLOBAL_OR_SERVER_INIT_EMOJI : SERVER_ONLY_CHANGE_INIT_EMOJI;
            long initId = isBotOwner ? GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT : SERVER_ONLY_PREFIX_CHANGE_INIT;
            if (super.addReactions(message, null, emojiList)) {
                SessionState sessionState = SessionState.forScenario(this, initId)
                        .setMessage(message)
                        .setRemoveReaction(true)
                        .setTextChannel(event.getChannel())
                        .setUser(user)
                        .build();
                super.commitState(sessionState);
            }
        });
    }

    @Override
    protected boolean privateMessageStep(@NotNull MessageCreateEvent event,
                                         @NotNull PrivateChannel privateChannel,
                                         @NotNull User user,
                                         @NotNull SessionState sessionState,
                                         boolean isBotOwner) {
        SettingsController settingsController = SettingsController.getInstance();
        boolean changeGlobal = isBotOwner && sessionState.stepIdIs(GLOBAL_PREFIX_AWAIT_ENTER);
        if (changeGlobal) {
            super.dropPreviousStateEmoji(sessionState);
            String newPrefix = event.getMessageContent().strip();
            String oldPrefix = settingsController.getGlobalCommonPrefix();
            if (isInvalidNewPrefix(newPrefix, oldPrefix, true, privateChannel))
                return false;
            displaySuccessPrefixChange(true, newPrefix, privateChannel);
            settingsController.setGlobalCommonPrefix(newPrefix);
            settingsController.saveCommonPreferences();
            return true;
        } else
            return false;
    }

    @Override
    protected boolean serverMessageStep(@NotNull MessageCreateEvent event,
                                        @NotNull Server server,
                                        @NotNull ServerTextChannel serverTextChannel,
                                        @NotNull User user,
                                        @NotNull SessionState sessionState,
                                        boolean isBowOwner) {
        SettingsController settingsController = SettingsController.getInstance();
        boolean changeServer = sessionState.stepIdIs(SERVER_PREFIX_AWAIT_ENTER);
        boolean changeGlobal = isBowOwner && sessionState.stepIdIs(GLOBAL_PREFIX_AWAIT_ENTER);
        if (changeGlobal || changeServer) {
            super.dropPreviousStateEmoji(sessionState);
            String newPrefix = event.getMessageContent().strip();
            String oldPrefix;
            if (changeGlobal) {
                oldPrefix = settingsController.getGlobalCommonPrefix();
            } else {
                oldPrefix = settingsController.getServerPreferences(server.getId()).getBotPrefix();
            }
            if (isInvalidNewPrefix(newPrefix, oldPrefix, changeGlobal, serverTextChannel))
                return false;
            displaySuccessPrefixChange(changeGlobal, newPrefix, serverTextChannel);
            if (changeGlobal) {
                settingsController.setGlobalCommonPrefix(newPrefix);
                settingsController.saveCommonPreferences();
            } else {
                settingsController.getServerPreferences(server.getId())
                        .setBotPrefix(newPrefix);
                settingsController.saveServerSideParameters(server.getId());
            }
            return true;
        } else
            return false;
    }

    private boolean isInvalidNewPrefix(@NotNull String newPrefix,
                                       @NotNull String oldPrefix,
                                       boolean changeGlobal,
                                       @NotNull TextChannel textChannel) {
        if (oldPrefix.equalsIgnoreCase(newPrefix)) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle(changeGlobal ? "Change global prefix" : "Change server prefix")
                    .setDescription("Old and new prefix is same. Please try again.\n" +
                            "Enter the new prefix with the new message. " +
                            "It is preferable to use a prefix that will be " +
                            "conveniently entered from the phone.");
            displayMessage(embedBuilder, textChannel).ifPresent(message ->
                    super.addReactions(message, null, ONLY_CLOSE_EMOJI));
            return true;
        } else {
            return false;
        }
    }

    private void displaySuccessPrefixChange(boolean changeGlobal,
                                            @NotNull String newPrefix,
                                            @NotNull TextChannel textChannel) {

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Bot commands prefix editor")
                .setDescription((changeGlobal ? "Global" : "Server")
                        + " bot prefix changed successfully")
                .addField("New prefix:", newPrefix);
        super.displayMessage(embedBuilder, textChannel);
    }

    @Override
    protected boolean privateReactionStep(boolean isAddReaction,
                                          @NotNull SingleReactionEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          @NotNull SessionState sessionState,
                                          boolean isBotOwner) {
        if (isCloseReaction(event, sessionState))
            return true;
        boolean isChangeGlobalReaction =
                super.equalsUnicodeReactions(event, EMOJI_CHANGE_GLOBAL, EMOJI_TWO_SELECT_GLOBAL)
                        && sessionState.stepIdIs(GLOBAL_ONLY_PREFIX_CHANGE_INIT, GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT)
                        && isBotOwner;
        if (isChangeGlobalReaction) {
            super.dropPreviousStateEmoji(sessionState);
            return sendInputPrefixDialog(true, event, sessionState);
        } else {
            removeRedundancyReaction(event, isAddReaction);
            return false;
        }
    }

    @Override
    protected boolean serverReactionStep(boolean isAddReaction,
                                         @NotNull SingleReactionEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         @NotNull SessionState sessionState,
                                         boolean isBotOwner) {
        if (isCloseReaction(event, sessionState))
            return true;
        boolean isChangeServerReaction =
                super.equalsUnicodeReactions(event, EMOJI_CHANGE_SERVER, EMOJI_ONE_SELECT_SERVER)
                        && sessionState.stepIdIs(SERVER_ONLY_PREFIX_CHANGE_INIT, GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT);
        boolean isChangeGlobalReaction =
                super.equalsUnicodeReactions(event, EMOJI_CHANGE_GLOBAL, EMOJI_TWO_SELECT_GLOBAL)
                        && sessionState.stepIdIs(GLOBAL_ONLY_PREFIX_CHANGE_INIT, GLOBAL_OR_SERVER_PREFIX_CHANGE_INIT)
                        && isBotOwner;
        if (isChangeServerReaction || isChangeGlobalReaction) {
            super.dropPreviousStateEmoji(sessionState);
            return sendInputPrefixDialog(isChangeGlobalReaction, event, sessionState);
        } else {
            removeRedundancyReaction(event, isAddReaction);
            return false;
        }
    }

    private void removeRedundancyReaction(@NotNull SingleReactionEvent event, boolean isAddReaction) {
        if (isAddReaction) {
            ((ReactionAddEvent) event).removeReaction();
        }
    }

    private boolean isCloseReaction(@NotNull SingleReactionEvent event,
                                    @NotNull SessionState sessionState) {
        if (equalsUnicodeReaction(event, EMOJI_CLOSE)) {
            super.dropPreviousStateEmoji(sessionState);
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle("Bot commands prefix editor")
                    .setDescription("Canceled");
            super.displayMessage(embedBuilder, event.getChannel());
            return true;
        } else
            return false;
    }

    private boolean sendInputPrefixDialog(boolean isChangeGlobalReaction,
                                          @NotNull SingleReactionEvent event,
                                          @NotNull SessionState sessionState) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(isChangeGlobalReaction ? "Change global prefix" : "Change server prefix")
                .setDescription("Enter the new prefix with the new message. " +
                        "It is preferable to use a prefix that will be " +
                        "conveniently entered from the phone.");
        return displayMessage(embedBuilder, event.getChannel()).map(message -> {
            if (super.addReactions(message, null, ONLY_CLOSE_EMOJI)) {
                SessionState newState = sessionState.toBuilderWithStepId(isChangeGlobalReaction ? GLOBAL_PREFIX_AWAIT_ENTER : SERVER_PREFIX_AWAIT_ENTER)
                        .setMessage(message)
                        .build();
                super.commitState(newState);
                return true;
            } else {
                return false;
            }
        }).orElse(false);
    }
}

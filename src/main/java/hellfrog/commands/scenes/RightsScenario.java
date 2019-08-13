package hellfrog.commands.scenes;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.commands.cmdline.BotCommand;
import hellfrog.common.BroadCast;
import hellfrog.core.SessionState;
import hellfrog.reacts.MsgCreateReaction;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RightsScenario
        extends Scenario {

    private static final String PREFIX = "rights";
    private static final String DESCRIPTION = "Display or change commands rights";

    private static final String TITLE = "Command permissions editor";
    private static final String SELECT_COMMAND_LIST_TEXT =
            "This command configures access rights to commands on this server.\n" +
                    "Enter the name of the bot command or reaction from the specified list:\n";
    private static final String HELP_TEXT =
            "The bot uses a command access model, which requires explicit " +
                    "permission to access any commands and reactions for users who " +
                    "do not have administrator rights and who do not own the server.\n" +
                    "Access is granted on a whitelist basis, i.e. " +
                    "forbidden everything that is clearly not allowed.\n" +
                    "There is no blacklist. If it is necessary to prohibit the user or role " +
                    "from executing any command or reaction, then it is necessary to " +
                    "remove the previously set permissive access.\n" +
                    "Some commands require explicit permission to execute in any " +
                    "specified text channels or channel categories. " +
                    "For bot commands that require explicit permission to work in a " +
                    "channel or category, there are two modes of access system " +
                    "operation (aka \"ACL mode\"): old and new.\n" +
                    "The old mode requires both explicit permission to execute in the " +
                    "text channel or category, and explicit permission for the user or role.\n" +
                    "The new mode requires only explicit permission to execute in a text " +
                    "channel or category. But at the same time, if desired, you can also " +
                    "set explicit permission for the user or role.";

    private static final String EMOJI_CLOSE = EmojiParser.parseToUnicode(":x:");
    private static final String EMOJI_QUESTION = EmojiParser.parseToUnicode(":question:");
    private static final String EMOJI_ARROW_LEFT = EmojiParser.parseToUnicode(":arrow_left:");
    private static final List<String> TITLE_EMOJI_LIST = List.of(EMOJI_QUESTION, EMOJI_CLOSE);
    private static final List<String> HELP_PAGE_EMOJI_LIST = List.of(EMOJI_ARROW_LEFT, EMOJI_CLOSE);

    private static final long STATE_ON_TITLE_SCREEN = 0L;
    private static final long STATE_ON_HELP_SCREEN = 1L;

    public RightsScenario() {
        super(PREFIX, DESCRIPTION);
        super.enableOnlyServerCommandStrict();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeServerFirstRun(@NotNull MessageCreateEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         boolean isBotOwner) {
        showTitleScreen(serverTextChannel, user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean serverMessageStep(@NotNull MessageCreateEvent event,
                                        @NotNull Server server,
                                        @NotNull ServerTextChannel serverTextChannel,
                                        @NotNull User user,
                                        @NotNull SessionState sessionState,
                                        boolean isBotOwner) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean serverReactionStep(boolean isAddReaction,
                                         @NotNull SingleReactionEvent event,
                                         @NotNull Server server,
                                         @NotNull ServerTextChannel serverTextChannel,
                                         @NotNull User user,
                                         @NotNull SessionState sessionState,
                                         boolean isBotOwner) {
        boolean onTitleStep = sessionState.stepIdIs(STATE_ON_TITLE_SCREEN);
        boolean onHelpScreen = sessionState.stepIdIs(STATE_ON_HELP_SCREEN);
        boolean userPressHelp = super.equalsUnicodeReaction(event, EMOJI_QUESTION);
        boolean userPressClose = super.equalsUnicodeReaction(event, EMOJI_CLOSE);
        boolean userPressArrowLeft = super.equalsUnicodeReaction(event, EMOJI_ARROW_LEFT);
        if (userPressClose) {
            showCancelledScreen(serverTextChannel, sessionState);
            return true;
        } else if (onTitleStep && userPressHelp) {
            super.dropPreviousStateEmoji(sessionState);
            return showHelpScreen(serverTextChannel, sessionState);
        } else if (onHelpScreen && userPressArrowLeft) {
            super.dropPreviousStateEmoji(sessionState);
            return showTitleScreen(serverTextChannel, user);
        }
        return false;
    }

    private boolean showTitleScreen(@NotNull ServerTextChannel serverTextChannel,
                                 @NotNull User user) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE);
        StringBuilder descriptionText = new StringBuilder()
                .append(SELECT_COMMAND_LIST_TEXT);
        descriptionText.append("*Bot commands:*\n");
        Scenario.all().forEach(scenario ->
                descriptionText.append('`').append(scenario.getPrefix()).append('`')
                        .append(" - ").append(scenario.getCommandDescription()).append('\n'));
        BotCommand.all().forEach(botCommand ->
                descriptionText.append('`').append(botCommand.getPrefix()).append('`')
                        .append(" - ").append(botCommand.getCommandDescription()).append('\n'));
        descriptionText.append("*Bot reactions:*\n");
        MsgCreateReaction.all().stream()
                .filter(MsgCreateReaction::isAccessControl)
                .forEachOrdered(msgCreateReaction ->
                        descriptionText.append('`').append(msgCreateReaction.getCommandPrefix()).append('`')
                                .append(" - ").append(msgCreateReaction.getCommandDescription()).append('\n'));
        descriptionText.append("\n")
                .append("You can also click on the ").append(EMOJI_QUESTION)
                .append(" to show help on the access rights of the bot. ")
                .append("Or click on the ")
                .append(EMOJI_CLOSE)
                .append(" to close the editor.");
        embedBuilder.setDescription(descriptionText.toString());
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
            if (super.addReactions(message, null, TITLE_EMOJI_LIST)) {
                SessionState sessionState = SessionState.forScenario(this, STATE_ON_TITLE_SCREEN)
                        .setMessage(message)
                        .setUser(user)
                        .setTextChannel(serverTextChannel)
                        .build();
                super.commitState(sessionState);
                return true;
            } else {
                return false;
            }
        }).orElse(false);
    }

    private void showCancelledScreen(@NotNull ServerTextChannel serverTextChannel,
                                     @NotNull SessionState sessionState) {
        super.dropPreviousStateEmoji(sessionState);
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE)
                .setDescription("Canceled");
        super.displayMessage(embedBuilder, serverTextChannel);
    }

    private boolean showHelpScreen(@NotNull ServerTextChannel serverTextChannel,
                                   @NotNull SessionState sessionState) {
        super.dropPreviousStateEmoji(sessionState);
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE);
        String descriptionText = HELP_TEXT +
                "\n\nClick on the " + EMOJI_ARROW_LEFT + " to return. " +
                "Or click on the " + EMOJI_CLOSE + " to close the editor.";
        embedBuilder.setDescription(descriptionText);
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
            if (super.addReactions(message, null, HELP_PAGE_EMOJI_LIST)) {
                SessionState newState = sessionState.toBuilderWithStepId(STATE_ON_HELP_SCREEN)
                        .setMessage(message)
                        .build();
                super.commitState(newState);
                return true;
            } else {
                return false;
            }
        }).orElse(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean privateReactionStep(boolean isAddReaction,
                                          @NotNull SingleReactionEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          @NotNull SessionState sessionState,
                                          boolean isBotOwner) {
        BroadCast.sendBroadcastToAllBotOwners("Exec of " + RightsScenario.class.getName()
                + "::privateReactionStep!");
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executePrivateFirstRun(@NotNull MessageCreateEvent event,
                                          @NotNull PrivateChannel privateChannel,
                                          @NotNull User user,
                                          boolean isBotOwner) {
        BroadCast.sendBroadcastToAllBotOwners("Exec of " + RightsScenario.class.getName()
                + "::executePrivateFirstRun!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean privateMessageStep(@NotNull MessageCreateEvent event,
                                         @NotNull PrivateChannel privateChannel,
                                         @NotNull User user,
                                         @NotNull SessionState sessionState,
                                         boolean isBotOwner) {
        BroadCast.sendBroadcastToAllBotOwners("Exec of " + RightsScenario.class.getName()
                + "::privateMessageStep!");
        return true;
    }
}

package hellfrog.commands.scenes;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.commands.cmdline.BotCommand;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.core.SessionState;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.CommandRights;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class RightsScenario
        extends Scenario {

    private static final String PREFIX = "rights";
    private static final String DESCRIPTION = "Display or change commands rights";

    private static final String TITLE = "Command permissions editor";
    private static final String SELECT_COMMAND_LIST_TEXT =
            "This command configures access rights to commands on this server.\n" +
                    "**Enter the name** of the bot command or reaction from the specified list:\n";
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
                    "set explicit permission for the user or role. " +
                    "The mode can be set only for all bot commands on " +
                    "the server as a whole.";

    private static final String EMOJI_CLOSE = EmojiParser.parseToUnicode(":x:");
    private static final String EMOJI_QUESTION = EmojiParser.parseToUnicode(":question:");
    private static final String EMOJI_ARROW_LEFT = EmojiParser.parseToUnicode(":arrow_left:");
    private static final String EMOJI_REPEAT = EmojiParser.parseToUnicode(":repeat:");
    private static final String EMOJI_PLUS = EmojiParser.parseToUnicode(":white_check_mark:");
    private static final String EMOJI_MINUS = EmojiParser.parseToUnicode(":negative_squared_cross_mark:");
    private static final String EMOJI_LETTER_U = EmojiParser.parseToUnicode(":regional_indicator_symbol_u:");
    private static final String EMOJI_LETTER_R = EmojiParser.parseToUnicode(":regional_indicator_symbol_r:");
    private static final String EMOJI_LETTER_T = EmojiParser.parseToUnicode(":regional_indicator_symbol_t:");
    private static final String EMOJI_LETTER_C = EmojiParser.parseToUnicode(":regional_indicator_symbol_c:");

    private static final List<String> TITLE_EMOJI_LIST =
            List.of(EMOJI_QUESTION, EMOJI_REPEAT, EMOJI_CLOSE);
    private static final List<String> HELP_PAGE_EMOJI_LIST =
            List.of(EMOJI_ARROW_LEFT, EMOJI_CLOSE);
    private static final List<String> ADD_ONLY_EMOJI_LIST =
            List.of(EMOJI_PLUS, EMOJI_ARROW_LEFT, EMOJI_CLOSE);
    private static final List<String> ADD_AND_REMOVE_EMOJI_LIST =
            List.of(EMOJI_PLUS, EMOJI_MINUS, EMOJI_ARROW_LEFT, EMOJI_CLOSE);
    private static final List<String> SELECT_ADD_EMOJI_LIST =
            List.of(EMOJI_LETTER_U, EMOJI_LETTER_R, EMOJI_ARROW_LEFT, EMOJI_CLOSE);
    private static final List<String> SELECT_ADD_WITH_CHAT_EMOJI_LIST =
            List.of(EMOJI_LETTER_U, EMOJI_LETTER_R, EMOJI_LETTER_T, EMOJI_LETTER_C, EMOJI_ARROW_LEFT, EMOJI_CLOSE);

    private static final long STATE_ON_TITLE_SCREEN = 0L;
    private static final long STATE_ON_HELP_SCREEN = 1L;
    private static final long STATE_ON_COMMAND_EDIT_SCREEN = 2L;
    private static final long STATE_ON_ADDITION_SELECT_SCREEN = 3L;
    private static final long STATE_ON_DELETION_SELECT_SCREEN = 4L;

    private static final String ENTERED_COMMAND_KEY = "command.name";

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
        boolean onTitleStep = sessionState.stepIdIs(STATE_ON_TITLE_SCREEN);
        if (onTitleStep) {
            super.dropPreviousStateEmoji(sessionState);
            return checkEnteredCommand(event, server, serverTextChannel, user);
        }
        return false;
    }

    private boolean checkEnteredCommand(@NotNull MessageCreateEvent event,
                                        @NotNull Server server,
                                        @NotNull ServerTextChannel serverTextChannel,
                                        @NotNull User user) {

        String rawInput = event.getReadableMessageContent().strip();
        Stream<String> scenarioPrefixes = Scenario.all().stream().map(Scenario::getPrefix);
        Stream<String> commandsPrefixes = BotCommand.all().stream().map(BotCommand::getPrefix);
        Stream<String> reactionPrefixes = MsgCreateReaction.all().stream()
                .filter(MsgCreateReaction::isAccessControl).map(MsgCreateReaction::getCommandPrefix);
        boolean isValidCommand = scenarioPrefixes.anyMatch(s -> s.equalsIgnoreCase(rawInput))
                || commandsPrefixes.anyMatch(s -> s.equalsIgnoreCase(rawInput))
                || reactionPrefixes.anyMatch(s -> s.equalsIgnoreCase(rawInput));
        if (isValidCommand) {
            return displayIfValidCommand(rawInput, server, serverTextChannel, user);
        } else {
            return displayIfInvalidCommand(rawInput, serverTextChannel, user);
        }
    }

    private boolean displayIfInvalidCommand(@NotNull String rawInput,
                                            @NotNull ServerTextChannel serverTextChannel,
                                            @NotNull User user) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        StringBuilder descriptionText = new StringBuilder();
        descriptionText.append("The specified command could not be recognized: `")
                .append(rawInput)
                .append("` . Please try again.");
        appendTitleDescription(descriptionText);
        embedBuilder.setTitle(TITLE)
                .setDescription(descriptionText.toString());
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

    private boolean displayIfValidCommand(@NotNull String rawInput,
                                          @NotNull Server server,
                                          @NotNull ServerTextChannel serverTextChannel,
                                          @NotNull User msgUser) {
        StringBuilder descriptionText = new StringBuilder();
        SettingsController settingsController = SettingsController.getInstance();
        CommandRights commandRights = settingsController
                .getServerPreferences(server.getId())
                .getRightsForCommand(rawInput);

        descriptionText.append("Command: __")
                .append(rawInput)
                .append("__:\n");

        Optional<String> allowedUsers = commandRights.getAllowUsers().stream()
                .map(userId -> {
                    Optional<User> mayBeUser = server.getMemberById(userId);
                    if (mayBeUser.isPresent()) {
                        User user = mayBeUser.get();
                        return server.getDisplayName(user)
                                + " (" + user.getDiscriminatedName() + ", id: " + userId + ")";
                    } else {
                        commandRights.delAllowUser(userId);
                        settingsController.saveServerSideParameters(server.getId());
                        return "[leaved-user, now removed from settings] (id: " + userId + ")";
                    }
                }).reduce(CommonUtils::reduceConcat);

        allowedUsers.ifPresent(s -> descriptionText.append("  * allow for users: ")
                .append(s)
                .append('\n'));

        Optional<String> allowedRoles = commandRights.getAllowRoles().stream()
                .map(roleId -> {
                    Optional<Role> mayBeRole = server.getRoleById(roleId);
                    if (mayBeRole.isPresent()) {
                        Role role = mayBeRole.get();
                        return role.getName() + " (id: " + roleId + ")";
                    } else {
                        commandRights.delAllowRole(roleId);
                        settingsController.saveServerSideParameters(server.getId());
                        return "[removed role, now removed from settings] (id: " + roleId + ")";
                    }
                }).reduce(CommonUtils::reduceConcat);

        allowedRoles.ifPresent(s -> descriptionText.append("  * allow for roles: ")
                .append(s)
                .append('\n'));

        Optional<String> allowedChannels = commandRights.getAllowChannels().stream()
                .map(channelId -> {
                    Optional<ServerTextChannel> mayBeChannel = server.getTextChannelById(channelId);
                    if (mayBeChannel.isPresent()) {
                        return this.getChannelNameAndId(mayBeChannel.get());
                    } else {
                        Optional<ChannelCategory> mayBeCategory = server.getChannelCategoryById(channelId);
                        if (mayBeCategory.isEmpty()) {
                            commandRights.delAllowChannel(channelId);
                            settingsController.saveServerSideParameters(server.getId());
                            return "[removed channel/category, now removed from settings] (id: " + channelId + ")";
                        } else {
                            return "";
                        }
                    }
                })
                .filter(s -> !CommonUtils.isTrStringEmpty(s))
                .reduce(CommonUtils::reduceConcat);

        allowedChannels.ifPresent(s -> descriptionText.append("  * allow for channels: ")
                .append(s)
                .append('\n'));

        Optional<String> allowedCategories = commandRights.getAllowChannels().stream()
                .map(channelId -> {
                    Optional<ChannelCategory> mayBeCategory = server.getChannelCategoryById(channelId);
                    if (mayBeCategory.isPresent()) {
                        return this.getChannelNameAndId(mayBeCategory.get());
                    } else {
                        Optional<ServerTextChannel> mayBeChannel = server.getTextChannelById(channelId);
                        if (mayBeChannel.isEmpty()) {
                            commandRights.delAllowChannel(channelId);
                            settingsController.saveServerSideParameters(server.getId());
                            return "[removed channel/category, now removed from settings] (id: " + channelId + ")";
                        } else {
                            return "";
                        }
                    }
                })
                .filter(s -> !CommonUtils.isTrStringEmpty(s))
                .reduce(CommonUtils::reduceConcat);

        allowedCategories.ifPresent(s -> descriptionText.append("  * allowed for categories: ")
                .append(s)
                .append('\n'));

        boolean isNotPermissionsSet = allowedUsers.isEmpty()
                && allowedRoles.isEmpty()
                && allowedChannels.isEmpty()
                && allowedCategories.isEmpty();
        List<String> emojiToAdd;
        if (isNotPermissionsSet) {
            descriptionText.append("  * No permissions set. A command can only be executed")
                    .append(" by administrators and the owner of the server.\n");
            emojiToAdd = ADD_ONLY_EMOJI_LIST;
        } else {
            emojiToAdd = ADD_AND_REMOVE_EMOJI_LIST;
        }
        descriptionText.append("Click on ").append(EMOJI_PLUS).append(" to add permission. ");
        if (!isNotPermissionsSet) {
            descriptionText.append("Click on ").append(EMOJI_MINUS).append(" to remove. ");
        }
        descriptionText.append("Click on ").append(EMOJI_ARROW_LEFT)
                .append(" to return to the input of another command. ")
                .append("Click on ").append(EMOJI_CLOSE).append(" to close the editor.");

        List<String> listOfMessagesText = CommonUtils.splitEqually(descriptionText.toString(), 1999);
        Message latest = null;
        for (String msgText : listOfMessagesText) {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle(TITLE)
                    .setDescription(msgText);
            Optional<Message> msg = super.displayMessage(embedBuilder, serverTextChannel);
            if (msg.isPresent()) {
                latest = msg.get();
            } else {
                return false;
            }
        }
        if (latest == null) {
            return false;
        }

        if (super.addReactions(latest, null, emojiToAdd)) {
            SessionState sessionState = SessionState.forScenario(this, STATE_ON_COMMAND_EDIT_SCREEN)
                    .setTextChannel(serverTextChannel)
                    .setMessage(latest)
                    .setUser(msgUser)
                    .putValue(ENTERED_COMMAND_KEY, rawInput)
                    .build();
            super.commitState(sessionState);
            return true;
        } else {
            return false;
        }
    }

    @NotNull
    private String getChannelNameAndId(@NotNull ServerChannel channel) {
        return channel.getName() + " (id: " + channel.getIdAsString() + ")";
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
        boolean onCommandEditScreen = sessionState.stepIdIs(STATE_ON_COMMAND_EDIT_SCREEN);
        boolean onAdditionSelectScreen = sessionState.stepIdIs(STATE_ON_ADDITION_SELECT_SCREEN)
                && sessionState.getValue(ENTERED_COMMAND_KEY, String.class) != null;
        boolean onDeletionSelectScreen = sessionState.stepIdIs(STATE_ON_DELETION_SELECT_SCREEN)
                && sessionState.getValue(ENTERED_COMMAND_KEY, String.class) != null;
        boolean userPressHelp = super.equalsUnicodeReaction(event, EMOJI_QUESTION);
        boolean userPressClose = super.equalsUnicodeReaction(event, EMOJI_CLOSE);
        boolean userPressArrowLeft = super.equalsUnicodeReaction(event, EMOJI_ARROW_LEFT);
        boolean userPressRepeat = super.equalsUnicodeReaction(event, EMOJI_REPEAT);
        boolean userPressAdd = super.equalsUnicodeReaction(event, EMOJI_PLUS);
        boolean userPressRemove = super.equalsUnicodeReaction(event, EMOJI_MINUS);

        if (userPressClose) {
            super.dropPreviousStateEmoji(sessionState);
            showCancelledScreen(serverTextChannel, sessionState);
            return true;
        } else if (onTitleStep && userPressHelp) {
            super.dropPreviousStateEmoji(sessionState);
            return showHelpScreen(serverTextChannel, sessionState);
        } else if ((onHelpScreen || onCommandEditScreen) && userPressArrowLeft) {
            super.dropPreviousStateEmoji(sessionState);
            return showTitleScreen(serverTextChannel, user);
        } else if (onTitleStep && userPressRepeat) {
            super.dropPreviousStateEmoji(sessionState);
            SettingsController settingsController = SettingsController.getInstance();
            ServerPreferences serverPreferences = settingsController
                    .getServerPreferences(server.getId());
            serverPreferences.setNewAclMode(!serverPreferences.getNewAclMode());
            settingsController.saveServerSideParameters(server.getId());
            return showTitleScreen(serverTextChannel, user);
        } else if (onCommandEditScreen && userPressAdd) {
            super.dropPreviousStateEmoji(sessionState);
            return selectAddition(serverTextChannel, sessionState);
        } else if (onCommandEditScreen && userPressRemove) {
            super.dropPreviousStateEmoji(sessionState);
            return selectDeletion(server, serverTextChannel, sessionState);
        } else if ((onAdditionSelectScreen || onDeletionSelectScreen) && userPressArrowLeft) {
            String rawCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
            if (!CommonUtils.isTrStringEmpty(rawCommandName)) {
                super.dropPreviousStateEmoji(sessionState);
                return displayIfValidCommand(rawCommandName, server, serverTextChannel, user);
            }
        }
        return false;
    }

    private boolean selectAddition(@NotNull ServerTextChannel serverTextChannel,
                                   @NotNull SessionState sessionState) {
        String enteredCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
        if (CommonUtils.isTrStringEmpty(enteredCommandName)) {
            return false;
        }

        boolean isStrictByChannels = checkCommandStrictByChannel(enteredCommandName);
        List<String> additionEmojiList = isStrictByChannels
                ? SELECT_ADD_WITH_CHAT_EMOJI_LIST
                : SELECT_ADD_EMOJI_LIST;
        StringBuilder descriptionText = new StringBuilder()
                .append("Command: __").append(enteredCommandName).append("__\n");
        if (isStrictByChannels) {
            descriptionText.append("For this command, the chat restriction is set or the chat category in ")
                    .append("which it can be executed.\n");
        }
        descriptionText.append("Select for which entity you want to **set** permission ")
                .append("to execute the command:\n")
                .append(EMOJI_LETTER_U).append(" - for user, ")
                .append(EMOJI_LETTER_R).append(" - for role");
        if (isStrictByChannels) {
            descriptionText.append(", ").append(EMOJI_LETTER_T).append(" - for text chat, ")
                    .append(EMOJI_LETTER_C).append(" - for category");
        }
        descriptionText.append(".\nYou can also click on ").append(EMOJI_ARROW_LEFT)
                .append(" to return to return to the previous menu and ")
                .append("click on ").append(EMOJI_CLOSE).append(" to close the editor.");
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE)
                .setDescription(descriptionText.toString());
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
            if (super.addReactions(message, null, additionEmojiList)) {
                SessionState newState = sessionState.toBuilderWithStepId(STATE_ON_ADDITION_SELECT_SCREEN)
                        .setMessage(message)
                        .build();
                super.commitState(newState);
                return true;
            } else {
                return false;
            }
        }).orElse(false);
    }

    private boolean selectDeletion(@NotNull Server server,
                                   @NotNull ServerTextChannel serverTextChannel,
                                   @NotNull SessionState sessionState) {
        String enteredCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
        if (CommonUtils.isTrStringEmpty(enteredCommandName)) {
            return false;
        }

        boolean isStrictByChannels = checkCommandStrictByChannel(enteredCommandName);
        List<String> emojiToAddition = new ArrayList<>();
        StringBuilder descriptionText = new StringBuilder()
                .append("Command: __").append(enteredCommandName).append("__\n");
        if (isStrictByChannels) {
            descriptionText.append("For this command, the chat restriction is set or the chat category in ")
                    .append("which it can be executed.\n");
        }
        descriptionText.append("Select for which entity you want to **remove** permission ")
                .append("to execute the command:\n");
        CommandRights commandRights = SettingsController.getInstance()
                .getServerPreferences(server.getId())
                .getRightsForCommand(enteredCommandName);
        boolean hasUsers = !commandRights.getAllowUsers().isEmpty()
                && commandRights.getAllowUsers().stream()
                .map(server::getMemberById)
                .anyMatch(Optional::isPresent);
        boolean hasRoles = !commandRights.getAllowRoles().isEmpty()
                && commandRights.getAllowRoles().stream()
                .map(server::getRoleById)
                .anyMatch(Optional::isPresent);
        boolean hasTextChannels = !commandRights.getAllowChannels().isEmpty()
                && commandRights.getAllowChannels().stream()
                .map(server::getTextChannelById)
                .anyMatch(Optional::isPresent);
        boolean hasChannelCategories = !commandRights.getAllowChannels().isEmpty()
                && commandRights.getAllowChannels().stream()
                .map(server::getChannelCategoryById)
                .anyMatch(Optional::isPresent);
        boolean addSeparator = false;
        if (hasUsers) {
            descriptionText.append(EMOJI_LETTER_U).append(" - for user");
            emojiToAddition.add(EMOJI_LETTER_U);
            addSeparator = true;
        }
        if (hasRoles) {
            if (addSeparator)
                descriptionText.append(", ");
            descriptionText.append(EMOJI_LETTER_R).append(" - for role");
            emojiToAddition.add(EMOJI_LETTER_R);
            addSeparator = true;
        }
        if (hasTextChannels) {
            if (addSeparator)
                descriptionText.append(", ");
            descriptionText.append(EMOJI_LETTER_T).append(" - for text channel");
            emojiToAddition.add(EMOJI_LETTER_T);
            addSeparator = true;
        }
        if (hasChannelCategories) {
            if (addSeparator)
                descriptionText.append(", ");
            descriptionText.append(EMOJI_LETTER_C).append(" - for channels category");
            emojiToAddition.add(EMOJI_LETTER_C);
        }
        descriptionText.append(".\nYou can also click on ").append(EMOJI_ARROW_LEFT)
                .append(" to return to return to the previous menu and ")
                .append("click on ").append(EMOJI_CLOSE).append(" to close the editor.");
        emojiToAddition.add(EMOJI_ARROW_LEFT);
        emojiToAddition.add(EMOJI_CLOSE);
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE)
                .setDescription(descriptionText.toString());
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
            if (super.addReactions(message, null, emojiToAddition)) {
                SessionState newState = sessionState.toBuilderWithStepId(STATE_ON_DELETION_SELECT_SCREEN)
                        .setMessage(message)
                        .build();
                super.commitState(newState);
                return true;
            } else {
                return false;
            }
        }).orElse(false);
    }

    private boolean checkCommandStrictByChannel(@NotNull String commandName) {
        return Scenario.all().stream()
                .filter(Scenario::isStrictByChannels)
                .map(Scenario::getPrefix)
                .anyMatch(s -> s.equalsIgnoreCase(commandName))
                || BotCommand.all().stream()
                .filter(BotCommand::isStrictByChannels)
                .map(BotCommand::getPrefix)
                .anyMatch(s -> s.equalsIgnoreCase(commandName))
                || MsgCreateReaction.all().stream()
                .filter(MsgCreateReaction::isAccessControl)
                .map(MsgCreateReaction::getCommandPrefix)
                .anyMatch(s -> s.equalsIgnoreCase(commandName));
    }

    private boolean showTitleScreen(@NotNull ServerTextChannel serverTextChannel,
                                    @NotNull User user) {
        boolean isNewACLServerMode = SettingsController.getInstance()
                .getServerPreferences(serverTextChannel.getServer().getId())
                .getNewAclMode();
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE);
        StringBuilder descriptionText = new StringBuilder()
                .append(SELECT_COMMAND_LIST_TEXT);
        descriptionText.append("__Bot commands:__\n");
        Scenario.all().forEach(scenario ->
                descriptionText.append('`').append(scenario.getPrefix()).append('`')
                        .append(" - ").append(scenario.getCommandDescription()).append('\n'));
        BotCommand.all().forEach(botCommand ->
                descriptionText.append('`').append(botCommand.getPrefix()).append('`')
                        .append(" - ").append(botCommand.getCommandDescription()).append('\n'));
        descriptionText.append("__Bot reactions:__\n");
        MsgCreateReaction.all().stream()
                .filter(MsgCreateReaction::isAccessControl)
                .forEachOrdered(msgCreateReaction ->
                        descriptionText.append('`').append(msgCreateReaction.getCommandPrefix()).append('`')
                                .append(" - ").append(msgCreateReaction.getCommandDescription()).append('\n'));
        appendTitleDescription(descriptionText);
        embedBuilder.setDescription(descriptionText.toString());
        embedBuilder.addField("Access system operation mode:",
                (isNewACLServerMode ? "New" : "Old"));
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

    private void appendTitleDescription(@NotNull StringBuilder stringBuilder) {
        stringBuilder.append("\n")
                .append("You can also click on the ").append(EMOJI_QUESTION)
                .append(" to show help on the access rights of the bot. ")
                .append("Or click on the ")
                .append(EMOJI_REPEAT)
                .append(" to switch access system operation. ")
                .append("Or click on the ")
                .append(EMOJI_CLOSE)
                .append(" to close the editor.");
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

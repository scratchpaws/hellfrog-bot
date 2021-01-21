package hellfrog.commands.scenes;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.commands.ACLCommand;
import hellfrog.commands.cmdline.BotCommand;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.common.LongEmbedMessage;
import hellfrog.core.AccessControlService;
import hellfrog.core.NameCacheService;
import hellfrog.core.ServerSideResolver;
import hellfrog.core.SessionState;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RightsScenario
        extends Scenario {

    private static final String PREFIX = "rights";
    private static final String DESCRIPTION = "Display or change commands rights";

    private static final String TITLE = "Command permissions editor";
    private static final String SELECT_COMMAND_LIST_TEXT =
            """
                    This command configures access rights to commands on this server.
                    **Enter the name** of the bot command or reaction from the specified list:
                    """;
    private static final String HELP_TEXT =
            """
                    The bot uses a command access model, which requires explicit permission to access any commands and reactions for users who do not have administrator rights and who do not own the server.
                    Access is granted on a whitelist basis, i.e. forbidden everything that is clearly not allowed.
                    There is no blacklist. If it is necessary to prohibit the user or role from executing any command or reaction, then it is necessary to remove the previously set permissive access.
                    Some commands require explicit permission to execute in any specified channels or channel categories. For bot commands that require explicit permission to work in a channel or category, there are two modes of access system operation (aka "ACL mode"): old and new.
                    The old mode requires both explicit permission to execute in the channel or category, and explicit permission for the user or role.
                    The new mode requires only explicit permission to execute in a channel or category. But at the same time, if desired, you can also set explicit permission for the user or role. The mode can be set only for all bot commands on the server as a whole.""";

    private static final String EMOJI_CLOSE = EmojiParser.parseToUnicode(":x:");
    private static final String EMOJI_QUESTION = EmojiParser.parseToUnicode(":question:");
    private static final String EMOJI_ARROW_LEFT = EmojiParser.parseToUnicode(":arrow_left:");
    private static final String EMOJI_REPEAT = EmojiParser.parseToUnicode(":repeat:");
    private static final String EMOJI_PLUS = EmojiParser.parseToUnicode(":white_check_mark:");
    private static final String EMOJI_MINUS = EmojiParser.parseToUnicode(":negative_squared_cross_mark:");
    private static final String EMOJI_SCROLL = EmojiParser.parseToUnicode(":scroll:");
    private static final String EMOJI_LETTER_U = EmojiParser.parseToUnicode(":regional_indicator_symbol_u:");
    private static final String EMOJI_LETTER_R = EmojiParser.parseToUnicode(":regional_indicator_symbol_r:");
    private static final String EMOJI_LETTER_T = EmojiParser.parseToUnicode(":regional_indicator_symbol_t:");
    private static final String EMOJI_LETTER_C = EmojiParser.parseToUnicode(":regional_indicator_symbol_c:");

    private static final List<String> TITLE_EMOJI_LIST =
            List.of(EMOJI_QUESTION, EMOJI_SCROLL, EMOJI_REPEAT, EMOJI_CLOSE);
    private static final List<String> GO_BACK_OR_CLOSE_EMOJI_LIST =
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
    private static final long STATE_ON_ENTER_ADD_RIGHT_ENTITY = 5L;
    private static final long STATE_ON_ENTER_DEL_RIGHT_ENTITY = 6L;

    private static final String ENTERED_COMMAND_KEY = "command.name";
    private static final String MODIFY_TYPE_KEY = "modify.right.type";

    private enum RightType {
        TYPE_NONE, TYPE_USER, TYPE_ROLE, TYPE_CHAT, TYPE_CATEGORY;

        static RightType ofEmoji(@NotNull SingleReactionEvent event) {
            boolean userPressUserLetter = equalsUnicodeReaction(event, EMOJI_LETTER_U);
            boolean userPressRoleLetter = equalsUnicodeReaction(event, EMOJI_LETTER_R);
            boolean userPressChatLetter = equalsUnicodeReaction(event, EMOJI_LETTER_T);
            boolean userPressCategoryLetter = equalsUnicodeReaction(event, EMOJI_LETTER_C);
            if (userPressUserLetter)
                return TYPE_USER;
            if (userPressRoleLetter)
                return TYPE_ROLE;
            if (userPressChatLetter)
                return TYPE_CHAT;
            if (userPressCategoryLetter)
                return TYPE_CATEGORY;

            return TYPE_NONE;
        }

        boolean isChatOrCategory() {
            return this.equals(TYPE_CHAT) || this.equals(TYPE_CATEGORY);
        }
    }

    public RightsScenario() {
        super(PREFIX, DESCRIPTION);
        super.setAdminCommand();
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

        String rawInput = event.getMessageContent();
        boolean rawInputNotEmpty = CommonUtils.isTrStringNotEmpty(rawInput);

        boolean sessionHasSavedCommand = sessionState.getValue(ENTERED_COMMAND_KEY, String.class) != null;
        boolean sessionHasSavedRightType = sessionState.getValue(MODIFY_TYPE_KEY, RightType.class) != null
                && !sessionState.getValue(MODIFY_TYPE_KEY, RightType.class).equals(RightType.TYPE_NONE);

        boolean onTitleStep = sessionState.stepIdIs(STATE_ON_TITLE_SCREEN);
        boolean onAddRightEntityScreen = sessionState.stepIdIs(STATE_ON_ENTER_ADD_RIGHT_ENTITY)
                && sessionHasSavedCommand && sessionHasSavedRightType;
        boolean onDelRightEntityScreen = sessionState.stepIdIs(STATE_ON_ENTER_DEL_RIGHT_ENTITY)
                && sessionHasSavedCommand && sessionHasSavedRightType;

        if (onTitleStep) {
            super.dropPreviousStateEmoji(sessionState);
            return checkEnteredCommand(event, server, serverTextChannel, user);
        } else if (onAddRightEntityScreen && rawInputNotEmpty) {
            super.dropPreviousStateEmoji(sessionState);
            return parseAdditionRightsEntity(server, serverTextChannel, sessionState, rawInput);
        } else if (onDelRightEntityScreen && rawInputNotEmpty) {
            super.dropPreviousStateEmoji(sessionState);
            return parseDeletionRightsEntity(server, serverTextChannel, user, sessionState, rawInput);
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

        LongEmbedMessage message = LongEmbedMessage.withTitleScenarioStyle(TITLE)
                .append("The specified command could not be recognized: `")
                .append(rawInput)
                .append("` . Please try again.");
        appendTitleDescription(message);
        return sendTitleScreen(serverTextChannel, user, message);
    }

    private boolean sendTitleScreen(@NotNull final ServerTextChannel serverTextChannel,
                                    @NotNull final User user,
                                    @NotNull final LongEmbedMessage messageToSend) {

        return super.displayMessage(messageToSend, serverTextChannel).map(sentMessage -> {
            if (super.addReactions(sentMessage, null, TITLE_EMOJI_LIST)) {
                SessionState sessionState = SessionState.forScenario(this, STATE_ON_TITLE_SCREEN)
                        .setMessage(sentMessage)
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

        LongEmbedMessage descriptionText = LongEmbedMessage.withTitleScenarioStyle(TITLE);

        AccessControlService ACL = SettingsController.getInstance().getAccessControlService();

        boolean isNotPermissionsSet = ACL.appendCommandRightsInfo(server, rawInput, descriptionText);

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

        Message latest = super.displayMessage(descriptionText, serverTextChannel).orElse(null);
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

    private boolean parseAdditionRightsEntity(@NotNull Server server,
                                              @NotNull ServerTextChannel serverTextChannel,
                                              @NotNull SessionState sessionState,
                                              @NotNull String rawInput) {

        String enteredCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
        if (CommonUtils.isTrStringEmpty(enteredCommandName)) {
            return false;
        }

        RightType rightType = sessionState.getValue(MODIFY_TYPE_KEY, RightType.class);
        if (rightType == null || rightType.equals(RightType.TYPE_NONE)) {
            return false;
        }

        List<String> entityNames = Arrays.stream(rawInput.split("\n"))
                .map(String::trim)
                .filter(CommonUtils::isTrStringNotEmpty)
                .collect(Collectors.toUnmodifiableList());

        LongEmbedMessage resultMessage = LongEmbedMessage.withTitleScenarioStyle(TITLE);

        if (entityNames.isEmpty()) {
            resultMessage.append("You have entered an empty name (or an empty list of names). ");
            return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
        }

        AccessControlService ACL = SettingsController.getInstance().getAccessControlService();
        NameCacheService nameCache = SettingsController.getInstance().getNameCacheService();

        switch (rightType) {
            case TYPE_USER -> {
                ServerSideResolver.ParseResult<User> userParseResult =
                        ServerSideResolver.resolveUsersList(server, entityNames);
                if (userParseResult.hasNotFound()) {
                    resultMessage.append("These users cannot be found: ")
                            .append(userParseResult.getNotFoundStringList()).append(". ");
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Collection<User> members = server.getMembers();
                Optional<String> nonServerUsers = userParseResult.getFound().stream()
                        .filter(parsedUser -> !members.contains(parsedUser))
                        .map(User::getDiscriminatedName)
                        .reduce(CommonUtils::reduceNewLine);
                if (nonServerUsers.isPresent()) {
                    resultMessage.append("These users are not present on this server:")
                            .appendNewLine().append(nonServerUsers.get())
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Optional<String> alreadyAssigned = userParseResult.getFound().stream()
                        .filter(parsedUser -> ACL.isAllowed(server, parsedUser, enteredCommandName))
                        .map(parsedUser -> nameCache.printEntityDetailed(parsedUser, server))
                        .reduce(CommonUtils::reduceNewLine);
                if (alreadyAssigned.isPresent()) {
                    resultMessage.append("These users are already allowed to execute the command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(':')
                            .appendNewLine().append(alreadyAssigned.get())
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                String usersList = userParseResult.getFound().stream()
                        .filter(user -> ACL.allow(server, user, enteredCommandName))
                        .map(user -> nameCache.printEntityDetailed(user, server))
                        .reduce(CommonUtils::reduceNewLine)
                        .orElse("");
                resultMessage.append("The following users are allowed to execute the command ")
                        .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                        .appendNewLine().append(usersList)
                        .appendNewLine();
            }
            case TYPE_ROLE -> {
                ServerSideResolver.ParseResult<Role> roleParseResult =
                        ServerSideResolver.resolveRolesList(server, entityNames);
                if (roleParseResult.hasNotFound()) {
                    resultMessage.append("These roles cannot be found: ")
                            .append(roleParseResult.getNotFoundStringList()).append(". ");
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Optional<String> alreadyAssigned = roleParseResult.getFound().stream()
                        .filter(role -> ACL.isAllowed(server, role, enteredCommandName))
                        .map(role -> nameCache.printEntityDetailed(role, server))
                        .reduce(CommonUtils::reduceNewLine);
                if (alreadyAssigned.isPresent()) {
                    resultMessage.append("These roles are already allowed to execute the command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                            .appendNewLine().appendIfPresent(alreadyAssigned)
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                String rolesList = roleParseResult.getFound().stream()
                        .filter(role -> ACL.allow(server, role, enteredCommandName))
                        .map(role -> nameCache.printEntityDetailed(role, server))
                        .reduce(CommonUtils::reduceNewLine)
                        .orElse("");
                resultMessage.append("The following roles are allowed to execute the command ")
                        .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                        .appendNewLine().append(rolesList)
                        .appendNewLine();
            }
            case TYPE_CHAT -> {
                ServerSideResolver.ParseResult<ServerChannel> channelParseResult =
                        ServerSideResolver.resolveNonCategoriesChannelsList(server, entityNames);
                if (channelParseResult.hasNotFound()) {
                    resultMessage.append("These channels cannot be found: ")
                            .append(channelParseResult.getNotFoundStringList()).append(". ");
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Optional<String> alreadyAssigned = channelParseResult.getFound().stream()
                        .filter(channel -> ACL.isAllowed(server, channel, enteredCommandName))
                        .map(channel -> nameCache.printEntityDetailed(channel, server))
                        .reduce(CommonUtils::reduceNewLine);
                if (alreadyAssigned.isPresent()) {
                    resultMessage.append("These channels are already allowed to execute the command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                            .appendNewLine().appendIfPresent(alreadyAssigned)
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                String channelsList = channelParseResult.getFound().stream()
                        .filter(channel -> ACL.allow(server, channel, enteredCommandName))
                        .map(channel -> nameCache.printEntityDetailed(channel, server))
                        .reduce(CommonUtils::reduceNewLine)
                        .orElse("");
                resultMessage.append("The following channels allow to execute the command ")
                        .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                        .appendNewLine().append(channelsList)
                        .appendNewLine();
            }
            case TYPE_CATEGORY -> {
                ServerSideResolver.ParseResult<ChannelCategory> categoryParseResult =
                        ServerSideResolver.resolveChannelCategoriesList(server, entityNames);
                if (categoryParseResult.hasNotFound()) {
                    resultMessage.append("These channel categories cannot be found: ")
                            .append(categoryParseResult.getNotFoundStringList()).append(". ");
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Optional<String> alreadyAssigned = categoryParseResult.getFound().stream()
                        .filter(channelCategory -> ACL.isAllowed(server, channelCategory, enteredCommandName))
                        .map(channelCategory -> nameCache.printEntityDetailed(channelCategory, server))
                        .reduce(CommonUtils::reduceNewLine);
                if (alreadyAssigned.isPresent()) {
                    resultMessage.append("These channel categories are already allowed to execute the command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                            .appendNewLine().appendIfPresent(alreadyAssigned)
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                String categoriesList = categoryParseResult.getFound().stream()
                        .filter(channelCategory -> ACL.allow(server, channelCategory, enteredCommandName))
                        .map(channelCategory -> nameCache.printEntityDetailed(channelCategory, server))
                        .reduce(CommonUtils::reduceNewLine)
                        .orElse("");
                resultMessage.append("For channels included in these channel categories, ")
                        .append("allowed to execute the command ").append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                        .appendNewLine().append(categoriesList)
                        .appendNewLine();
            }
        }

        return super.displayMessage(resultMessage, serverTextChannel).map(message -> {
            SessionState newSessionState = sessionState.toBuilderWithStepId(STATE_ON_COMMAND_EDIT_SCREEN)
                    .putValue(MODIFY_TYPE_KEY, RightType.TYPE_NONE)
                    .build();
            return selectAddition(server, serverTextChannel, newSessionState);
        }).orElse(false);
    }

    private boolean parseDeletionRightsEntity(@NotNull Server server,
                                              @NotNull ServerTextChannel serverTextChannel,
                                              @NotNull User messageAuthorUser,
                                              @NotNull SessionState sessionState,
                                              @NotNull String rawInput) {

        String enteredCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
        if (CommonUtils.isTrStringEmpty(enteredCommandName)) {
            return false;
        }

        RightType rightType = sessionState.getValue(MODIFY_TYPE_KEY, RightType.class);
        if (rightType == null || rightType.equals(RightType.TYPE_NONE)) {
            return false;
        }

        List<String> entityNames = Arrays.stream(rawInput.split("\n"))
                .map(String::trim)
                .filter(CommonUtils::isTrStringNotEmpty)
                .collect(Collectors.toUnmodifiableList());

        LongEmbedMessage resultMessage = LongEmbedMessage.withTitleScenarioStyle(TITLE);

        if (entityNames.isEmpty()) {
            resultMessage.append("You have entered an empty name (or an empty list of names). ");
            return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
        }

        AccessControlService ACL = SettingsController.getInstance().getAccessControlService();
        NameCacheService nameCache = SettingsController.getInstance().getNameCacheService();

        if (ACL.notHasAnyRights(server, enteredCommandName)) {
            super.dropPreviousStateEmoji(sessionState);
            resultMessage.append("There are currently no users, roles, or text chat (categories) that are ")
                    .append("allowed to run command ").append(enteredCommandName, MessageDecoration.UNDERLINE).append(".");
            return super.displayMessage(resultMessage, serverTextChannel).map(message ->
                    displayIfValidCommand(enteredCommandName, server, serverTextChannel, messageAuthorUser)
            ).orElse(false);
        }

        switch (rightType) {
            case TYPE_USER -> {
                if (ACL.noHasUsersRights(server, enteredCommandName)) {
                    resultMessage.append("There are currently no users who are allowed to execute command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(".");
                    return displayErrorNoEntityForDelete(server, serverTextChannel, sessionState, resultMessage);
                }
                ServerSideResolver.ParseResult<User> userParseResult =
                        ServerSideResolver.resolveUsersList(server, entityNames);
                if (userParseResult.hasNotFound()) {
                    resultMessage.append("These users cannot be found: ").append(userParseResult.getNotFoundStringList())
                            .append(". ");
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Optional<String> alreadyNotAllowed = userParseResult.getFound().stream()
                        .filter(parsedUser -> !ACL.isAllowed(server, parsedUser, enteredCommandName))
                        .map(parsedUser -> nameCache.printEntityDetailed(parsedUser, server))
                        .reduce(CommonUtils::reduceNewLine);
                if (alreadyNotAllowed.isPresent()) {
                    resultMessage.append("These users are already ").append("not", MessageDecoration.BOLD)
                            .append(" allowed to execute the command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                            .appendNewLine().appendIfPresent(alreadyNotAllowed)
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                String usersList = userParseResult.getFound().stream()
                        .filter(user -> ACL.deny(server, user, enteredCommandName))
                        .map(user -> nameCache.printEntityDetailed(user, server))
                        .reduce(CommonUtils::reduceNewLine)
                        .orElse("");
                resultMessage.append("The right to execute the command ")
                        .append(enteredCommandName, MessageDecoration.UNDERLINE)
                        .append(" was revoked for the following users:")
                        .appendNewLine().append(usersList)
                        .appendNewLine();
            }
            case TYPE_ROLE -> {
                if (ACL.noHasRolesRights(server, enteredCommandName)) {
                    resultMessage.append("There are currently no roles who are allowed to execute command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(".");
                    return displayErrorNoEntityForDelete(server, serverTextChannel, sessionState, resultMessage);
                }
                ServerSideResolver.ParseResult<Role> roleParseResult =
                        ServerSideResolver.resolveRolesList(server, entityNames);
                if (roleParseResult.hasNotFound()) {
                    resultMessage.append("These roles cannot be found: ").append(roleParseResult.getNotFoundStringList())
                            .append(". ");
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Optional<String> alreadyNotAllowed = roleParseResult.getFound().stream()
                        .filter(role -> !ACL.isAllowed(server, role, enteredCommandName))
                        .map(role -> nameCache.printEntityDetailed(role, server))
                        .reduce(CommonUtils::reduceNewLine);
                if (alreadyNotAllowed.isPresent()) {
                    resultMessage.append("These roles are already ").append("not", MessageDecoration.BOLD)
                            .append(" allowed to execute the command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                            .appendNewLine().appendIfPresent(alreadyNotAllowed)
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                String rolesList = roleParseResult.getFound().stream()
                        .filter(role -> ACL.deny(server, role, enteredCommandName))
                        .map(role -> nameCache.printEntityDetailed(role, server))
                        .reduce(CommonUtils::reduceNewLine)
                        .orElse("");
                resultMessage.append("The right to execute the command ").append(enteredCommandName, MessageDecoration.UNDERLINE)
                        .append(" was revoked for the following roles:")
                        .appendNewLine().append(rolesList)
                        .appendNewLine();
            }
            case TYPE_CHAT -> {
                if (ACL.noHasChannelRights(server, enteredCommandName)) {
                    resultMessage.append("There are currently no channels who are allowed to execute command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(".");
                    return displayErrorNoEntityForDelete(server, serverTextChannel, sessionState, resultMessage);
                }
                ServerSideResolver.ParseResult<ServerChannel> channelParseResult =
                        ServerSideResolver.resolveNonCategoriesChannelsList(server, entityNames);
                if (channelParseResult.hasNotFound()) {
                    resultMessage.append("These channels cannot be found: ")
                            .append(channelParseResult.getNotFoundStringList()).append(". ");
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Optional<String> alreadyNotAllowed = channelParseResult.getFound().stream()
                        .filter(channel -> !ACL.isAllowed(server, channel, enteredCommandName))
                        .map(channel -> nameCache.printEntityDetailed(channel, server))
                        .reduce(CommonUtils::reduceNewLine);
                if (alreadyNotAllowed.isPresent()) {
                    resultMessage.append("These channels are already ").append("not", MessageDecoration.BOLD)
                            .append(" allowed to execute the command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                            .appendNewLine().appendIfPresent(alreadyNotAllowed)
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                String channelsList = channelParseResult.getFound().stream()
                        .filter(channel -> ACL.deny(server, channel, enteredCommandName))
                        .map(channel -> nameCache.printEntityDetailed(channel, server))
                        .reduce(CommonUtils::reduceNewLine)
                        .orElse("");
                resultMessage.append("The right to execute the command ").append(enteredCommandName, MessageDecoration.UNDERLINE)
                        .append(" was revoked for the following channels:")
                        .appendNewLine().append(channelsList)
                        .appendNewLine();
            }
            case TYPE_CATEGORY -> {
                if (ACL.noHasCategoryRights(server, enteredCommandName)) {
                    resultMessage.append("There are currently no channel categories who are allowed to execute command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(".");
                    return displayErrorNoEntityForDelete(server, serverTextChannel, sessionState, resultMessage);
                }
                ServerSideResolver.ParseResult<ChannelCategory> categoryParseResult =
                        ServerSideResolver.resolveChannelCategoriesList(server, entityNames);
                if (categoryParseResult.hasNotFound()) {
                    resultMessage.append("These channel categories cannot be found: ")
                            .append(categoryParseResult.getNotFoundStringList()).append(". ");
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                Optional<String> alreadyNotAllowed = categoryParseResult.getFound().stream()
                        .filter(channelCategory -> !ACL.isAllowed(server, channelCategory, enteredCommandName))
                        .map(channelCategory -> nameCache.printEntityDetailed(channelCategory, server))
                        .reduce(CommonUtils::reduceNewLine);
                if (alreadyNotAllowed.isPresent()) {
                    resultMessage.append("These channel categories are already ").append("not", MessageDecoration.BOLD)
                            .append(" allowed to execute the command ")
                            .append(enteredCommandName, MessageDecoration.UNDERLINE).append(":")
                            .appendNewLine().appendIfPresent(alreadyNotAllowed)
                            .appendNewLine();
                    return displayEnterEntityError(serverTextChannel, sessionState, resultMessage);
                }
                String categoriesList = categoryParseResult.getFound().stream()
                        .filter(channelCategory -> ACL.deny(server, channelCategory, enteredCommandName))
                        .map(channelCategory -> nameCache.printEntityDetailed(channelCategory, server))
                        .reduce(CommonUtils::reduceNewLine)
                        .orElse("");
                resultMessage.append("The right to execute a command ").append(enteredCommandName, MessageDecoration.UNDERLINE)
                        .append(" has been revoked for the following ")
                        .append("categories (and all channels that include these categories):")
                        .appendNewLine().append(categoriesList)
                        .appendNewLine();
            }
        }

        return super.displayMessage(resultMessage, serverTextChannel).map(message -> {
            SessionState newSessionState = sessionState.toBuilderWithStepId(STATE_ON_COMMAND_EDIT_SCREEN)
                    .putValue(MODIFY_TYPE_KEY, RightType.TYPE_NONE)
                    .build();
            return selectDeletion(server, serverTextChannel, newSessionState);
        }).orElse(false);
    }

    private boolean displayEnterEntityError(@NotNull ServerTextChannel serverTextChannel,
                                            @NotNull SessionState sessionState,
                                            @NotNull LongEmbedMessage resultMessage) {

        resultMessage.append("Please try again.\nYou can also click on ").append(EMOJI_ARROW_LEFT)
                .append(" to return to return to the previous menu and ")
                .append("click on ").append(EMOJI_CLOSE).append(" to close the editor.");

        return super.displayMessage(resultMessage, serverTextChannel).map(message -> {
            if (super.addReactions(message, null, GO_BACK_OR_CLOSE_EMOJI_LIST)) {
                SessionState newState = sessionState.toBuilder()
                        .setMessage(message)
                        .build();
                super.commitState(newState);
                return true;
            } else {
                return false;
            }
        }).orElse(false);
    }

    private boolean displayErrorNoEntityForDelete(@NotNull Server server,
                                                  @NotNull ServerTextChannel serverTextChannel,
                                                  @NotNull SessionState sessionState,
                                                  @NotNull LongEmbedMessage errorMessage) {

        return super.displayMessage(errorMessage, serverTextChannel).map(message -> {
            SessionState newSessionState = sessionState.toBuilderWithStepId(STATE_ON_COMMAND_EDIT_SCREEN)
                    .putValue(MODIFY_TYPE_KEY, RightType.TYPE_NONE)
                    .build();
            return selectDeletion(server, serverTextChannel, newSessionState);
        }).orElse(false);
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

        boolean sessionHasSavedCommand = sessionState.getValue(ENTERED_COMMAND_KEY, String.class) != null;
        boolean sessionHasSavedRightType = sessionState.getValue(MODIFY_TYPE_KEY, RightType.class) != null
                && !sessionState.getValue(MODIFY_TYPE_KEY, RightType.class).equals(RightType.TYPE_NONE);

        boolean onTitleStep = sessionState.stepIdIs(STATE_ON_TITLE_SCREEN);
        boolean onHelpScreen = sessionState.stepIdIs(STATE_ON_HELP_SCREEN);
        boolean onCommandEditScreen = sessionState.stepIdIs(STATE_ON_COMMAND_EDIT_SCREEN);
        boolean onAdditionSelectScreen = sessionState.stepIdIs(STATE_ON_ADDITION_SELECT_SCREEN)
                && sessionHasSavedCommand;
        boolean onDeletionSelectScreen = sessionState.stepIdIs(STATE_ON_DELETION_SELECT_SCREEN)
                && sessionHasSavedCommand;
        boolean onAddRightEntityScreen = sessionState.stepIdIs(STATE_ON_ENTER_ADD_RIGHT_ENTITY)
                && sessionHasSavedCommand && sessionHasSavedRightType;
        boolean onDelRightEntityScreen = sessionState.stepIdIs(STATE_ON_ENTER_DEL_RIGHT_ENTITY)
                && sessionHasSavedCommand && sessionHasSavedRightType;

        boolean userPressHelp = equalsUnicodeReaction(event, EMOJI_QUESTION);
        boolean userPressClose = equalsUnicodeReaction(event, EMOJI_CLOSE);
        boolean userPressArrowLeft = equalsUnicodeReaction(event, EMOJI_ARROW_LEFT);
        boolean userPressRepeat = equalsUnicodeReaction(event, EMOJI_REPEAT);
        boolean userPressAdd = equalsUnicodeReaction(event, EMOJI_PLUS);
        boolean userPressRemove = equalsUnicodeReaction(event, EMOJI_MINUS);
        boolean userPressScroll = equalsUnicodeReaction(event, EMOJI_SCROLL);

        RightType rightType = RightType.ofEmoji(event);

        if (userPressClose) {
            super.dropPreviousStateEmoji(sessionState);
            showCancelledScreen(serverTextChannel, sessionState);
            return true;
        } else if (onTitleStep && userPressHelp) {
            super.dropPreviousStateEmoji(sessionState);
            return showHelpScreen(serverTextChannel, sessionState);
        } else if (onTitleStep && userPressScroll) {
            super.dropPreviousStateEmoji(sessionState);
            return displayAllCommandsWithRights(server, serverTextChannel, user);
        } else if ((onHelpScreen || onCommandEditScreen) && userPressArrowLeft) {
            super.dropPreviousStateEmoji(sessionState);
            return showTitleScreen(serverTextChannel, user);
        } else if (onTitleStep && userPressRepeat) {
            super.dropPreviousStateEmoji(sessionState);
            AccessControlService ACL = SettingsController.getInstance().getAccessControlService();
            ACL.setNewAclMode(server, !ACL.isNewAclMode(server));
            return showTitleScreen(serverTextChannel, user);
        } else if (onCommandEditScreen && userPressAdd) {
            super.dropPreviousStateEmoji(sessionState);
            return selectAddition(server, serverTextChannel, sessionState);
        } else if (onCommandEditScreen && userPressRemove) {
            super.dropPreviousStateEmoji(sessionState);
            return selectDeletion(server, serverTextChannel, sessionState);
        } else if ((onAdditionSelectScreen || onDeletionSelectScreen) && userPressArrowLeft) {
            String rawCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
            if (!CommonUtils.isTrStringEmpty(rawCommandName)) {
                super.dropPreviousStateEmoji(sessionState);
                return displayIfValidCommand(rawCommandName, server, serverTextChannel, user);
            }
        } else if (onAdditionSelectScreen && !rightType.equals(RightType.TYPE_NONE)) {
            super.dropPreviousStateEmoji(sessionState);
            return showAddScreen(server, serverTextChannel, sessionState, rightType);
        } else if (onDeletionSelectScreen && !rightType.equals(RightType.TYPE_NONE)) {
            super.dropPreviousStateEmoji(sessionState);
            return showDeleteScreen(server, serverTextChannel, sessionState, rightType);
        } else if (onAddRightEntityScreen && userPressArrowLeft) {
            super.dropPreviousStateEmoji(sessionState);
            sessionState.putValue(MODIFY_TYPE_KEY, RightType.TYPE_NONE);
            return selectAddition(server, serverTextChannel, sessionState);
        } else if (onDelRightEntityScreen && userPressArrowLeft) {
            super.dropPreviousStateEmoji(sessionState);
            sessionState.putValue(MODIFY_TYPE_KEY, RightType.TYPE_NONE);
            return selectDeletion(server, serverTextChannel, sessionState);
        }
        return false;
    }

    private boolean displayAllCommandsWithRights(@NotNull Server server,
                                                 @NotNull ServerTextChannel serverTextChannel,
                                                 @NotNull User user) {

        LongEmbedMessage descriptionText = LongEmbedMessage.withTitleInfoStyle(TITLE);

        descriptionText.append("The following commands have any specified permission on the server `")
                .append(ServerSideResolver.getReadableContent(server.getName(), Optional.of(server)))
                .append("`:\n");

        AccessControlService accessControlService = SettingsController.getInstance().getAccessControlService();
        accessControlService.getAllCommandPrefix()
                .forEach(command -> accessControlService.appendCommandRightsInfo(server, command, descriptionText));

        appendTitleDescription(descriptionText);

        Message latest = super.displayMessage(descriptionText, serverTextChannel).orElse(null);
        if (latest == null) {
            return false;
        }

        if (super.addReactions(latest, null, TITLE_EMOJI_LIST)) {
            SessionState sessionState = SessionState.forScenario(this, STATE_ON_TITLE_SCREEN)
                    .setMessage(latest)
                    .setUser(user)
                    .setTextChannel(serverTextChannel)
                    .build();
            super.commitState(sessionState);
            return true;
        } else {
            return false;
        }
    }

    private boolean selectAddition(@NotNull final Server server,
                                   @NotNull final ServerTextChannel serverTextChannel,
                                   @NotNull final SessionState sessionState) {
        String enteredCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
        if (CommonUtils.isTrStringEmpty(enteredCommandName)) {
            return false;
        }

        boolean isStrictByChannels = SettingsController.getInstance()
                .getAccessControlService()
                .isStrictByChannelsOnServer(server, enteredCommandName);
        List<String> additionEmojiList = isStrictByChannels
                ? SELECT_ADD_WITH_CHAT_EMOJI_LIST
                : SELECT_ADD_EMOJI_LIST;
        LongEmbedMessage descriptionText = LongEmbedMessage.withTitleScenarioStyle(TITLE)
                .append("Command: ").append(enteredCommandName, MessageDecoration.UNDERLINE).appendNewLine();
        if (isStrictByChannels) {
            descriptionText.append("For this command, the chat restriction is set or the chat category in ")
                    .append("which it can be executed.").appendNewLine();
        }
        descriptionText.append("Select for which entity you want to ")
                .append("set", MessageDecoration.BOLD).append(" permission ")
                .append("to execute the command:").appendNewLine()
                .append(EMOJI_LETTER_U).append(" - for user, ")
                .append(EMOJI_LETTER_R).append(" - for role");
        if (isStrictByChannels) {
            descriptionText.append(", ").append(EMOJI_LETTER_T).append(" - for channel, ")
                    .append(EMOJI_LETTER_C).append(" - for category");
        }
        descriptionText.append(".").appendNewLine()
                .append("You can also click on ").append(EMOJI_ARROW_LEFT)
                .append(" to return to return to the previous menu and ")
                .append("click on ").append(EMOJI_CLOSE).append(" to close the editor.");

        return super.displayMessage(descriptionText, serverTextChannel).map(message -> {
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

        AccessControlService ACL = SettingsController.getInstance().getAccessControlService();

        List<String> emojiToAddition = new ArrayList<>();
        LongEmbedMessage descriptionText = LongEmbedMessage.withTitleScenarioStyle(TITLE)
                .append("Command: ").append(enteredCommandName, MessageDecoration.UNDERLINE).appendNewLine();
        if (ACL.isStrictByChannelsOnServer(server, enteredCommandName)) {
            descriptionText.append("For this command, the chat restriction is set or the chat category in ")
                    .append("which it can be executed.").appendNewLine();
        }
        descriptionText.append("Select for which entity you want to ")
                .append("remove", MessageDecoration.BOLD).append(" permission ")
                .append("to execute the command:").appendNewLine();

        boolean addSeparator = false;
        if (ACL.hasUsersRights(server, enteredCommandName)) {
            descriptionText.append(EMOJI_LETTER_U).append(" - for user");
            emojiToAddition.add(EMOJI_LETTER_U);
            addSeparator = true;
        }
        if (ACL.hasRolesRights(server, enteredCommandName)) {
            if (addSeparator)
                descriptionText.append(", ");
            descriptionText.append(EMOJI_LETTER_R).append(" - for role");
            emojiToAddition.add(EMOJI_LETTER_R);
            addSeparator = true;
        }
        if (ACL.hasChannelRights(server, enteredCommandName)) {
            if (addSeparator)
                descriptionText.append(", ");
            descriptionText.append(EMOJI_LETTER_T).append(" - for channel");
            emojiToAddition.add(EMOJI_LETTER_T);
            addSeparator = true;
        }
        if (ACL.hasCategoryRights(server, enteredCommandName)) {
            if (addSeparator)
                descriptionText.append(", ");
            descriptionText.append(EMOJI_LETTER_C).append(" - for channels category");
            emojiToAddition.add(EMOJI_LETTER_C);
        }
        descriptionText.append(".").appendNewLine()
                .append("You can also click on ").append(EMOJI_ARROW_LEFT)
                .append(" to return to return to the previous menu and ")
                .append("click on ").append(EMOJI_CLOSE).append(" to close the editor.");
        emojiToAddition.add(EMOJI_ARROW_LEFT);
        emojiToAddition.add(EMOJI_CLOSE);

        return super.displayMessage(descriptionText, serverTextChannel).map(message -> {
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

    private boolean showAddScreen(@NotNull final Server server,
                                  @NotNull final ServerTextChannel serverTextChannel,
                                  @NotNull final SessionState sessionState,
                                  @NotNull final RightType rightType) {
        String enteredCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
        if (CommonUtils.isTrStringEmpty(enteredCommandName)) {
            return false;
        }
        boolean isStrictByChannel = SettingsController.getInstance()
                .getAccessControlService()
                .isStrictByChannelsOnServer(server, enteredCommandName);
        if (!isStrictByChannel && rightType.isChatOrCategory()) {
            return false;
        }
        if (rightType.equals(RightType.TYPE_NONE)) {
            return false;
        }
        LongEmbedMessage descriptionText = LongEmbedMessage.withTitleInfoStyle(TITLE);
        addRightAddDelDescription(descriptionText, rightType, true);

        return super.displayMessage(descriptionText, serverTextChannel).map(message -> {
            if (super.addReactions(message, null, GO_BACK_OR_CLOSE_EMOJI_LIST)) {
                SessionState newState = sessionState.toBuilderWithStepId(STATE_ON_ENTER_ADD_RIGHT_ENTITY)
                        .putValue(MODIFY_TYPE_KEY, rightType)
                        .setMessage(message)
                        .build();
                super.commitState(newState);
                return true;
            } else {
                return false;
            }
        }).orElse(false);
    }

    private boolean showDeleteScreen(@NotNull Server server,
                                     @NotNull ServerTextChannel serverTextChannel,
                                     @NotNull SessionState sessionState,
                                     @NotNull RightType rightType) {

        String enteredCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
        if (CommonUtils.isTrStringEmpty(enteredCommandName)) {
            return false;
        }
        AccessControlService ACL = SettingsController.getInstance().getAccessControlService();
        boolean canDeleteThisType = false;
        switch (rightType) {
            case TYPE_USER:
                if (ACL.hasUsersRights(server, enteredCommandName))
                    canDeleteThisType = true;
                break;

            case TYPE_ROLE:
                if (ACL.hasRolesRights(server, enteredCommandName))
                    canDeleteThisType = true;
                break;

            case TYPE_CHAT:
                if (ACL.hasChannelRights(server, enteredCommandName))
                    canDeleteThisType = true;
                break;

            case TYPE_CATEGORY:
                if (ACL.hasCategoryRights(server, enteredCommandName))
                    canDeleteThisType = true;
                break;
        }
        LongEmbedMessage descriptionText = LongEmbedMessage.withTitleInfoStyle(TITLE);
        if (!canDeleteThisType) {
            descriptionText.append("Unfortunately, but the list of ");
            switch (rightType) {
                case TYPE_USER -> descriptionText.append("users who are allowed to run this command is empty.");
                case TYPE_ROLE -> descriptionText.append("roles that are allowed to run this command is empty.");
                case TYPE_CHAT -> descriptionText.append("channels in which the execution of this command " +
                        "is allowed is empty.");
                case TYPE_CATEGORY -> descriptionText.append("categories in the channels of which " +
                        "the execution of this command is allowed is empty.");
            }
            return super.displayMessage(descriptionText, serverTextChannel).map(message ->
                    selectDeletion(server, serverTextChannel, sessionState)
            ).orElse(false);
        } else {
            addRightAddDelDescription(descriptionText, rightType, false);
            return super.displayMessage(descriptionText, serverTextChannel).map(message -> {
                if (super.addReactions(message, null, GO_BACK_OR_CLOSE_EMOJI_LIST)) {
                    SessionState newState = sessionState.toBuilderWithStepId(STATE_ON_ENTER_DEL_RIGHT_ENTITY)
                            .putValue(MODIFY_TYPE_KEY, rightType)
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

    private void addRightAddDelDescription(@NotNull final LongEmbedMessage descriptionText,
                                           @NotNull final RightType rightType,
                                           final boolean isAdditionRights) {

        final String grant = "grant";
        final String revoke = "revoke";
        String wordReplace = isAdditionRights ? grant : revoke;

        switch (rightType) {
            case TYPE_USER -> descriptionText.append("Enter the user to whom you want to ")
                    .append(wordReplace, MessageDecoration.BOLD)
                    .append(" access to the specified command. ")
                    .append("You can specify (listed in priority order when performing ")
                    .append("a user search with a bot): user ID, its mention, discriminating name ")
                    .append("(i.e. global\\_username#user\\_tag), global username ")
                    .append("(i.e. without #user\\_tag), nickname on this server.");
            case TYPE_ROLE -> descriptionText.append("Enter the role that you want to ")
                    .append(wordReplace, MessageDecoration.BOLD)
                    .append(" access to the specified command. ")
                    .append("You can specify (listed in order of priority when ")
                    .append("performing a search for a role by a bot): role ID, its mention, ")
                    .append("role name.");
            case TYPE_CHAT -> {
                descriptionText.append("Enter the channel that you want to ")
                        .append(wordReplace, MessageDecoration.BOLD)
                        .append(" the execution of the specified command. ");
                if (!isAdditionRights) {
                    descriptionText.append("Keep in mind that permission to execute commands for a category ")
                            .append("of channels override the lack of rights to execute commands ")
                            .append("for channels in this category. ");
                }
                descriptionText.append("You can specify (listed in order of priority when ")
                        .append("performing a channel search with a bot): channel ID, its mention, ")
                        .append("channel name.");
            }
            case TYPE_CATEGORY -> {
                descriptionText.append("Enter the channel category in which you want to ")
                        .append(wordReplace, MessageDecoration.BOLD)
                        .append(" the execution of the specified command. ");
                if (isAdditionRights) {
                    descriptionText.append("Keep in mind that the permission applies ")
                            .append("to all channels in this category (it is impossible to selectively ")
                            .append("prohibit the execution of a command on any channel in this ")
                            .append("category while the permission is valid for the entire category). ");
                } else {
                    descriptionText.append("Keep in mind that permission to execute commands for a category ")
                            .append("of channels override the lack of rights to execute commands ")
                            .append("for channels in this category. ");
                }
                descriptionText.append("You can specify (listed in order of priority when performing a channel ")
                        .append("search with a bot): category ID, category name.");
            }
        }

        descriptionText.appendNewLine()
                .append("Entering a value is not case sensitive. ")
                .append("If there are two of the same name, the first one will be selected. ")
                .append("You can list several at once, on each line individually. ")
                .append("Use `Shift`+`Enter` to jump to a new line.").appendNewLine()
                .append("You can also click on ").append(EMOJI_ARROW_LEFT)
                .append(" to return to return to the previous menu and ")
                .append("click on ").append(EMOJI_CLOSE).append(" to close the editor.");
    }

    private boolean showTitleScreen(@NotNull final ServerTextChannel serverTextChannel,
                                    @NotNull final User user) {
        AccessControlService ACL = SettingsController.getInstance().getAccessControlService();
        LongEmbedMessage descriptionText = LongEmbedMessage.withTitleScenarioStyle(TITLE)
                .append(SELECT_COMMAND_LIST_TEXT);
        descriptionText.append("Bot commands:", MessageDecoration.UNDERLINE).appendNewLine();
        Scenario.all().stream()
                .filter(ACLCommand::isNotExpertCommand)
                .forEach(scenario ->
                        descriptionText.append(scenario.getPrefix(), MessageDecoration.CODE_SIMPLE)
                                .append(" - ").append(scenario.getCommandDescription()).appendNewLine());
        BotCommand.all().stream()
                .filter(ACLCommand::isNotExpertCommand)
                .forEach(botCommand ->
                        descriptionText.append(botCommand.getPrefix(), MessageDecoration.CODE_SIMPLE)
                                .append(" - ").append(botCommand.getCommandDescription()).appendNewLine());
        descriptionText.append("Bot reactions:", MessageDecoration.UNDERLINE).appendNewLine();
        MsgCreateReaction.all().stream()
                .filter(MsgCreateReaction::isAccessControl)
                .forEachOrdered(msgCreateReaction ->
                        descriptionText.append(msgCreateReaction.getCommandPrefix(), MessageDecoration.CODE_SIMPLE)
                                .append(" - ").append(msgCreateReaction.getCommandDescription()).appendNewLine());
        descriptionText.append("Bot expert commands:", MessageDecoration.UNDERLINE).appendNewLine();
        Scenario.all().stream()
                .filter(ACLCommand::isExpertCommand)
                .forEach(scenario ->
                        descriptionText.append(scenario.getPrefix(), MessageDecoration.CODE_SIMPLE)
                                .append(" - ").append(scenario.getCommandDescription()).appendNewLine());
        BotCommand.all().stream()
                .filter(ACLCommand::isExpertCommand)
                .forEach(botCommand ->
                        descriptionText.append(botCommand.getPrefix(), MessageDecoration.CODE_SIMPLE)
                                .append(" - ").append(botCommand.getCommandDescription()).appendNewLine());
        appendTitleDescription(descriptionText);
        descriptionText.addField("Access system operation mode:",
                (ACL.isNewAclMode(serverTextChannel.getServer()) ? "New" : "Old"));
        return sendTitleScreen(serverTextChannel, user, descriptionText);
    }

    private void appendTitleDescription(@NotNull LongEmbedMessage message) {
        message.append("\n")
                .append("You can also click on the ").append(EMOJI_QUESTION)
                .append(" to show help on the access rights of the bot. ")
                .append(" Or click on the ")
                .append(EMOJI_SCROLL)
                .append(" to display all the commands for which any rights are granted. ")
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
                .setDescription("Goodbye.");
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
            if (super.addReactions(message, null, GO_BACK_OR_CLOSE_EMOJI_LIST)) {
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
        BroadCast.getLogger()
                .addUnsafeUsageCE("executed " + RightsScenario.class.getName() + "::privateReactionStep!", event)
                .send();
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
        BroadCast.getLogger()
                .addUnsafeUsageCE("executed " + RightsScenario.class.getName() + "::executePrivateFirstRun!", event)
                .send();
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
        BroadCast.getLogger()
                .addUnsafeUsageCE("executed " + RightsScenario.class.getName() + "::privateMessageStep!", event)
                .send();
        return true;
    }
}

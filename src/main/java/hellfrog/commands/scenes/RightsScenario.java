package hellfrog.commands.scenes;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.commands.ACLCommand;
import hellfrog.commands.cmdline.BotCommand;
import hellfrog.common.BroadCast;
import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
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
                    Some commands require explicit permission to execute in any specified text channels or channel categories. For bot commands that require explicit permission to work in a channel or category, there are two modes of access system operation (aka "ACL mode"): old and new.
                    The old mode requires both explicit permission to execute in the text channel or category, and explicit permission for the user or role.
                    The new mode requires only explicit permission to execute in a text channel or category. But at the same time, if desired, you can also set explicit permission for the user or role. The mode can be set only for all bot commands on the server as a whole.""";

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
        TYPE_NONE, TYPE_USER, TYPE_ROLE, TYPE_TEXT_CHAT, TYPE_CATEGORY;

        static RightType ofEmoji(@NotNull SingleReactionEvent event) {
            boolean userPressUserLetter = equalsUnicodeReaction(event, EMOJI_LETTER_U);
            boolean userPressRoleLetter = equalsUnicodeReaction(event, EMOJI_LETTER_R);
            boolean userPressTextChatLetter = equalsUnicodeReaction(event, EMOJI_LETTER_T);
            boolean userPressCategoryLetter = equalsUnicodeReaction(event, EMOJI_LETTER_C);
            if (userPressUserLetter)
                return TYPE_USER;
            if (userPressRoleLetter)
                return TYPE_ROLE;
            if (userPressTextChatLetter)
                return TYPE_TEXT_CHAT;
            if (userPressCategoryLetter)
                return TYPE_CATEGORY;

            return TYPE_NONE;
        }

        boolean in(@NotNull RightType that1, @NotNull RightType that2) {
            return this.equals(that1) || this.equals(that2);
        }
    }

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

        boolean isNotPermissionsSet = appendCommandRightsInfo(descriptionText,
                settingsController, server, rawInput);

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

    private boolean appendCommandRightsInfo(@NotNull StringBuilder descriptionText,
                                            @NotNull SettingsController settingsController,
                                            @NotNull Server server,
                                            @NotNull String commandName) {

        CommandRights commandRights = settingsController
                .getServerPreferences(server.getId())
                .getRightsForCommand(commandName);

        descriptionText.append("Command: __")
                .append(commandName)
                .append("__:\n");

        Optional<String> allowedUsers = commandRights.getAllowUsers().stream()
                .map(userId -> {
                    Optional<User> mayBeUser = server.getMemberById(userId);
                    if (mayBeUser.isPresent()) {
                        return getUserNameAndId(server, mayBeUser.get());
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

        return allowedUsers.isEmpty()
                && allowedRoles.isEmpty()
                && allowedChannels.isEmpty()
                && allowedCategories.isEmpty();
    }

    @NotNull
    private String getChannelNameAndId(@NotNull ServerChannel channel) {
        return channel.getName() + " (id: " + channel.getIdAsString() + ")";
    }

    @NotNull
    private String getUserNameAndId(@NotNull Server server, @NotNull User user) {
        return server.getDisplayName(user)
                + " (" + user.getDiscriminatedName() + ", id: " + user.getId() + ")";
    }

    @NotNull
    private String getRoleNameAndId(@NotNull Role role) {
        return role.getName() + " (id: " + role.getId() + ")";
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

        if (entityNames.isEmpty()) {
            return displayEnterEntityError(serverTextChannel, sessionState,
                    "You have entered an empty name (or an empty list of names).");
        }

        SettingsController settingsController = SettingsController.getInstance();
        ServerPreferences serverPreferences = settingsController.getServerPreferences(server.getId());
        CommandRights commandRights = serverPreferences.getRightsForCommand(enteredCommandName);

        String resultMessage = "";

        switch (rightType) {

            case TYPE_USER:
                ServerSideResolver.ParseResult<User> userParseResult =
                        ServerSideResolver.resolveUsersList(server, entityNames);
                if (userParseResult.hasNotFound()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These users cannot be found: "
                                    + userParseResult.getNotFoundStringList());
                }
                Collection<User> members = server.getMembers();
                Optional<String> nonServerUsers = userParseResult.getFound().stream()
                        .filter(parsedUser -> !members.contains(parsedUser))
                        .map(User::getDiscriminatedName)
                        .reduce(CommonUtils::reduceConcat);
                if (nonServerUsers.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These users are not present on this server: "
                                    + nonServerUsers.get());
                }
                Optional<String> alreadyAssigned = userParseResult.getFound().stream()
                        .filter(parsedUser -> commandRights.isAllowUser(parsedUser.getId()))
                        .map(parsedUser -> getUserNameAndId(server, parsedUser))
                        .reduce(CommonUtils::reduceConcat);
                if (alreadyAssigned.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These users are already allowed to execute the command __"
                                    + enteredCommandName + "__: " + alreadyAssigned.get());
                }
                userParseResult.getFound().forEach(user -> commandRights.getAllowUsers().add(user.getId()));
                settingsController.saveServerSideParameters(server.getId());
                String usersList = userParseResult.getFound().stream()
                        .map(user -> getUserNameAndId(server, user))
                        .reduce(CommonUtils::reduceConcat)
                        .orElse("");
                resultMessage = "The following users are allowed to execute the command __"
                        + enteredCommandName + "__: " + usersList;
                break;

            case TYPE_ROLE:
                ServerSideResolver.ParseResult<Role> roleParseResult =
                        ServerSideResolver.resolveRolesList(server, entityNames);
                if (roleParseResult.hasNotFound()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These roles cannot be found: "
                                    + roleParseResult.getNotFoundStringList());
                }
                alreadyAssigned = roleParseResult.getFound().stream()
                        .filter(role -> commandRights.isAllowRole(role.getId()))
                        .map(this::getRoleNameAndId)
                        .reduce(CommonUtils::reduceConcat);
                if (alreadyAssigned.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These roles are already allowed to execute the command __"
                                    + enteredCommandName + "__: " + alreadyAssigned.get());
                }
                roleParseResult.getFound().forEach(role -> commandRights.getAllowRoles().add(role.getId()));
                settingsController.saveServerSideParameters(server.getId());
                String rolesList = roleParseResult.getFound().stream()
                        .map(this::getRoleNameAndId)
                        .reduce(CommonUtils::reduceConcat)
                        .orElse("");
                resultMessage = "The following roles are allowed to execute the command __"
                        + enteredCommandName + "__: " + rolesList;
                break;

            case TYPE_TEXT_CHAT:
                ServerSideResolver.ParseResult<ServerTextChannel> textChannelParseResult =
                        ServerSideResolver.resolveTextChannelsList(server, entityNames);
                if (textChannelParseResult.hasNotFound()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These text channels cannot be found: "
                                    + textChannelParseResult.getNotFoundStringList());
                }
                alreadyAssigned = textChannelParseResult.getFound().stream()
                        .filter(textChannel -> commandRights.isAllowChat(textChannel.getId()))
                        .map(this::getChannelNameAndId)
                        .reduce(CommonUtils::reduceConcat);
                if (alreadyAssigned.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These text channels are already allowed to execute the command __"
                                    + enteredCommandName + "__: " + alreadyAssigned.get());
                }
                textChannelParseResult.getFound().forEach(textChannel ->
                        commandRights.addAllowChannel(textChannel.getId()));
                settingsController.saveServerSideParameters(server.getId());
                String channelsList = textChannelParseResult.getFound().stream()
                        .map(this::getChannelNameAndId)
                        .reduce(CommonUtils::reduceConcat)
                        .orElse("");
                resultMessage = "The following text channels allow to execute the command __"
                        + enteredCommandName + "__: " + channelsList;
                break;

            case TYPE_CATEGORY:
                ServerSideResolver.ParseResult<ChannelCategory> categoryParseResult =
                        ServerSideResolver.resolveChannelCategoriesList(server, entityNames);
                if (categoryParseResult.hasNotFound()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These channel categories cannot be found: "
                                    + categoryParseResult.getNotFoundStringList());
                }
                alreadyAssigned = categoryParseResult.getFound().stream()
                        .filter(channelCategory -> commandRights.isAllowChat(channelCategory.getId()))
                        .map(this::getChannelNameAndId)
                        .reduce(CommonUtils::reduceConcat);
                if (alreadyAssigned.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These channel categories are already allowed to execute the command __"
                                    + enteredCommandName + "__: " + alreadyAssigned.get());
                }
                categoryParseResult.getFound().forEach(channelCategory ->
                        commandRights.addAllowChannel(channelCategory.getId()));
                settingsController.saveServerSideParameters(server.getId());
                String categoriesList = categoryParseResult.getFound().stream()
                        .map(this::getChannelNameAndId)
                        .reduce(CommonUtils::reduceConcat)
                        .orElse("");
                resultMessage = "For text channels included in these channel categories, " +
                        "allowed to execute the command __" + enteredCommandName + "__: " + categoriesList;
                break;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE)
                .setDescription(resultMessage);
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
            SessionState newSessionState = sessionState.toBuilderWithStepId(STATE_ON_COMMAND_EDIT_SCREEN)
                    .putValue(MODIFY_TYPE_KEY, RightType.TYPE_NONE)
                    .build();
            return selectAddition(serverTextChannel, newSessionState);
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

        if (entityNames.isEmpty()) {
            return displayEnterEntityError(serverTextChannel, sessionState,
                    "You have entered an empty name (or an empty list of names).");
        }

        SettingsController settingsController = SettingsController.getInstance();
        ServerPreferences serverPreferences = settingsController.getServerPreferences(server.getId());
        CommandRights commandRights = serverPreferences.getRightsForCommand(enteredCommandName);
        ShortRightsInfo rightsInfo = new ShortRightsInfo(server, enteredCommandName);

        if (!rightsInfo.isThereAnything) {
            super.dropPreviousStateEmoji(sessionState);
            String descriptionMessage = "There are currently no users, roles, or text chat (categories) that are " +
                    "allowed to run this command.";
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle(TITLE)
                    .setDescription(descriptionMessage);
            return super.displayMessage(embedBuilder, serverTextChannel).map(message ->
                    displayIfValidCommand(enteredCommandName, server, serverTextChannel, messageAuthorUser)
            ).orElse(false);
        }

        String resultMessage = "";

        switch (rightType) {
            case TYPE_USER:
                if (!rightsInfo.hasUsers) {
                    String message = "There are currently no users who are allowed to execute this command.";
                    return displayErrorNoEntityForDelete(server, serverTextChannel, sessionState, message);
                }
                ServerSideResolver.ParseResult<User> userParseResult =
                        ServerSideResolver.resolveUsersList(server, entityNames);
                if (userParseResult.hasNotFound()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These users cannot be found: "
                                    + userParseResult.getNotFoundStringList());
                }
                Collection<User> members = server.getMembers();
                Optional<String> nonServerUsers = userParseResult.getFound().stream()
                        .filter(parsedUser -> !members.contains(parsedUser))
                        .map(User::getDiscriminatedName)
                        .reduce(CommonUtils::reduceConcat);
                if (nonServerUsers.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These users are not present on this server: "
                                    + nonServerUsers.get());
                }
                Optional<String> alreadyNotAllowed = userParseResult.getFound().stream()
                        .filter(parsedUser -> !commandRights.isAllowUser(parsedUser.getId()))
                        .map(parsedUser -> getUserNameAndId(server, parsedUser))
                        .reduce(CommonUtils::reduceConcat);
                if (alreadyNotAllowed.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These users are already **not** allowed to execute the command __"
                                    + enteredCommandName + "__: " + alreadyNotAllowed.get());
                }
                List<Long> toRemoveIds = userParseResult.getFound().stream()
                        .map(User::getId)
                        .collect(Collectors.toUnmodifiableList());
                commandRights.getAllowUsers().removeAll(toRemoveIds);
                settingsController.saveServerSideParameters(server.getId());
                String usersList = userParseResult.getFound().stream()
                        .map(user -> getUserNameAndId(server, user))
                        .reduce(CommonUtils::reduceConcat)
                        .orElse("");
                resultMessage = "The right to execute the command __" + enteredCommandName
                        + "__ was revoked for the following users: " + usersList;
                break;

            case TYPE_ROLE:
                if (!rightsInfo.hasRoles) {
                    String message = "There are currently no roles who are allowed to execute this command.";
                    return displayErrorNoEntityForDelete(server, serverTextChannel, sessionState, message);
                }
                ServerSideResolver.ParseResult<Role> roleParseResult =
                        ServerSideResolver.resolveRolesList(server, entityNames);
                if (roleParseResult.hasNotFound()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These roles cannot be found: "
                                    + roleParseResult.getNotFoundStringList());
                }
                alreadyNotAllowed = roleParseResult.getFound().stream()
                        .filter(role -> !commandRights.isAllowRole(role.getId()))
                        .map(this::getRoleNameAndId)
                        .reduce(CommonUtils::reduceConcat);
                if (alreadyNotAllowed.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These roles are already **not** allowed to execute the command __"
                                    + enteredCommandName + "__: " + alreadyNotAllowed.get());
                }
                toRemoveIds = roleParseResult.getFound().stream()
                        .map(Role::getId)
                        .collect(Collectors.toUnmodifiableList());
                commandRights.getAllowRoles().removeAll(toRemoveIds);
                settingsController.saveServerSideParameters(server.getId());
                String rolesList = roleParseResult.getFound().stream()
                        .map(this::getRoleNameAndId)
                        .reduce(CommonUtils::reduceConcat)
                        .orElse("");
                resultMessage = "The right to execute the command __" + enteredCommandName
                        + "__ was revoked for the following roles: " + rolesList;
                break;

            case TYPE_TEXT_CHAT:
                if (!rightsInfo.hasTextChannels) {
                    String message = "There are currently no text channels in which the execution of this " +
                            "command is allowed.";
                    return displayErrorNoEntityForDelete(server, serverTextChannel, sessionState, message);
                }
                ServerSideResolver.ParseResult<ServerTextChannel> textChannelParseResult =
                        ServerSideResolver.resolveTextChannelsList(server, entityNames);
                if (textChannelParseResult.hasNotFound()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These text channels cannot be found: "
                                    + textChannelParseResult.getNotFoundStringList());
                }
                alreadyNotAllowed = textChannelParseResult.getFound().stream()
                        .filter(textChannel -> !commandRights.isAllowChat(textChannel.getId()))
                        .map(this::getChannelNameAndId)
                        .reduce(CommonUtils::reduceConcat);
                if (alreadyNotAllowed.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These text channels are already **not** allowed to execute the command __"
                                    + enteredCommandName + "__: " + alreadyNotAllowed.get());
                }
                toRemoveIds = textChannelParseResult.getFound().stream()
                        .map(ServerTextChannel::getId)
                        .collect(Collectors.toUnmodifiableList());
                commandRights.getAllowChannels().removeAll(toRemoveIds);
                settingsController.saveServerSideParameters(server.getId());
                String channelsList = textChannelParseResult.getFound().stream()
                        .map(this::getChannelNameAndId)
                        .reduce(CommonUtils::reduceConcat)
                        .orElse("");
                resultMessage = "The right to execute the command __" + enteredCommandName
                        + "__ was revoked for the following text channels: " + channelsList;
                break;

            case TYPE_CATEGORY:
                if (!rightsInfo.hasChannelCategories) {
                    String message = "There are currently no categories in which text chats are allowed to " +
                            "execute this command.";
                    return displayErrorNoEntityForDelete(server, serverTextChannel, sessionState, message);
                }
                ServerSideResolver.ParseResult<ChannelCategory> categoryParseResult =
                        ServerSideResolver.resolveChannelCategoriesList(server, entityNames);
                if (categoryParseResult.hasNotFound()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These channel categories cannot be found: "
                                    + categoryParseResult.getNotFoundStringList());
                }
                alreadyNotAllowed = categoryParseResult.getFound().stream()
                        .filter(channelCategory -> !commandRights.isAllowChat(channelCategory.getId()))
                        .map(this::getChannelNameAndId)
                        .reduce(CommonUtils::reduceConcat);
                if (alreadyNotAllowed.isPresent()) {
                    return displayEnterEntityError(serverTextChannel, sessionState,
                            "These channel categories are already **not** allowed to execute the command __"
                                    + enteredCommandName + "__: " + alreadyNotAllowed.get());
                }
                toRemoveIds = categoryParseResult.getFound().stream()
                        .map(ChannelCategory::getId)
                        .collect(Collectors.toUnmodifiableList());
                commandRights.getAllowChannels().removeAll(toRemoveIds);
                settingsController.saveServerSideParameters(server.getId());
                String categoriesList = categoryParseResult.getFound().stream()
                        .map(this::getChannelNameAndId)
                        .reduce(CommonUtils::reduceConcat)
                        .orElse("");
                resultMessage = "The right to execute a command __" + enteredCommandName
                        + "__ has been revoked for the following " +
                        "categories (and all text channels that include these categories): " + categoriesList;
                break;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE)
                .setDescription(resultMessage);
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
            SessionState newSessionState = sessionState.toBuilderWithStepId(STATE_ON_COMMAND_EDIT_SCREEN)
                    .putValue(MODIFY_TYPE_KEY, RightType.TYPE_NONE)
                    .build();
            return selectDeletion(server, serverTextChannel, newSessionState);
        }).orElse(false);
    }

    private boolean displayEnterEntityError(@NotNull ServerTextChannel serverTextChannel,
                                            @NotNull SessionState sessionState,
                                            @NotNull String errorText) {

        String descriptionMessage = errorText +
                ". Please try again.\nYou can also click on " + EMOJI_ARROW_LEFT +
                " to return to return to the previous menu and " +
                "click on " + EMOJI_CLOSE + " to close the editor.";

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE)
                .setDescription(descriptionMessage);
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
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
                                                  @NotNull String errorMessage) {

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE)
                .setDescription(errorMessage);
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
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
        } else if (onAdditionSelectScreen && !rightType.equals(RightType.TYPE_NONE)) {
            super.dropPreviousStateEmoji(sessionState);
            return showAddScreen(serverTextChannel, sessionState, rightType);
        } else if (onDeletionSelectScreen && !rightType.equals(RightType.TYPE_NONE)) {
            super.dropPreviousStateEmoji(sessionState);
            return showDeleteScreen(server, serverTextChannel, sessionState, rightType);
        } else if (onAddRightEntityScreen && userPressArrowLeft) {
            super.dropPreviousStateEmoji(sessionState);
            sessionState.putValue(MODIFY_TYPE_KEY, RightType.TYPE_NONE);
            return selectAddition(serverTextChannel, sessionState);
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
        StringBuilder descriptionText = new StringBuilder();
        descriptionText.append("The following commands have any specified permission on the server `")
                .append(ServerSideResolver.getReadableContent(server.getName(), Optional.of(server)))
                .append("`:\n");
        SettingsController settingsController = SettingsController.getInstance();
        ServerPreferences serverPreferences = settingsController.getServerPreferences(server.getId());
        serverPreferences.getSrvCommandRights().keySet().stream()
                .filter(item ->
                        new ShortRightsInfo(server, item).isThereAnything)
                .forEachOrdered(item ->
                        appendCommandRightsInfo(descriptionText, settingsController, server, item));
        appendTitleDescription(descriptionText);

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
        ShortRightsInfo rightsInfo = new ShortRightsInfo(server, enteredCommandName);
        boolean addSeparator = false;
        if (rightsInfo.hasUsers) {
            descriptionText.append(EMOJI_LETTER_U).append(" - for user");
            emojiToAddition.add(EMOJI_LETTER_U);
            addSeparator = true;
        }
        if (rightsInfo.hasRoles) {
            if (addSeparator)
                descriptionText.append(", ");
            descriptionText.append(EMOJI_LETTER_R).append(" - for role");
            emojiToAddition.add(EMOJI_LETTER_R);
            addSeparator = true;
        }
        if (rightsInfo.hasTextChannels) {
            if (addSeparator)
                descriptionText.append(", ");
            descriptionText.append(EMOJI_LETTER_T).append(" - for text channel");
            emojiToAddition.add(EMOJI_LETTER_T);
            addSeparator = true;
        }
        if (rightsInfo.hasChannelCategories) {
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

    private boolean showAddScreen(@NotNull ServerTextChannel serverTextChannel,
                                  @NotNull SessionState sessionState,
                                  @NotNull RightType rightType) {
        String enteredCommandName = sessionState.getValue(ENTERED_COMMAND_KEY, String.class);
        if (CommonUtils.isTrStringEmpty(enteredCommandName)) {
            return false;
        }
        boolean isStrictByChannel = checkCommandStrictByChannel(enteredCommandName);
        if (!isStrictByChannel && rightType.in(RightType.TYPE_TEXT_CHAT, RightType.TYPE_CATEGORY)) {
            return false;
        }
        if (rightType.equals(RightType.TYPE_NONE)) {
            return false;
        }
        StringBuilder descriptionText = new StringBuilder();
        addRightAddDelDescription(descriptionText, rightType, true);

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE)
                .setDescription(descriptionText.toString());
        return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
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
        ShortRightsInfo rightsInfo = new ShortRightsInfo(server, enteredCommandName);
        boolean canDeleteThisType = false;
        switch (rightType) {
            case TYPE_USER:
                if (rightsInfo.hasUsers)
                    canDeleteThisType = true;
                break;

            case TYPE_ROLE:
                if (rightsInfo.hasRoles)
                    canDeleteThisType = true;
                break;

            case TYPE_TEXT_CHAT:
                if (rightsInfo.hasTextChannels)
                    canDeleteThisType = true;
                break;

            case TYPE_CATEGORY:
                if (rightsInfo.hasChannelCategories)
                    canDeleteThisType = true;
                break;
        }
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(TITLE);
        StringBuilder descriptionText = new StringBuilder();
        if (!canDeleteThisType) {
            descriptionText.append("Unfortunately, but the list of ");
            switch (rightType) {
                case TYPE_USER -> descriptionText.append("users who are allowed to run this command is empty.");
                case TYPE_ROLE -> descriptionText.append("roles that are allowed to run this command is empty.");
                case TYPE_TEXT_CHAT -> descriptionText.append("text channels in which the execution of this command " +
                        "is allowed is empty.");
                case TYPE_CATEGORY -> descriptionText.append("categories in the text channels of which " +
                        "the execution of this command is allowed is empty.");
            }
            embedBuilder.setDescription(descriptionText.toString());
            return super.displayMessage(embedBuilder, serverTextChannel).map(message ->
                    selectDeletion(server, serverTextChannel, sessionState)
            ).orElse(false);
        } else {
            addRightAddDelDescription(descriptionText, rightType, false);
            embedBuilder.setDescription(descriptionText.toString());
            return super.displayMessage(embedBuilder, serverTextChannel).map(message -> {
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

    private void addRightAddDelDescription(@NotNull StringBuilder descriptionText,
                                           @NotNull RightType rightType,
                                           boolean isAdditionRights) {

        final String grant = "**grant**";
        final String revoke = "**revoke**";
        String wordReplace = isAdditionRights ? grant : revoke;

        switch (rightType) {
            case TYPE_USER -> descriptionText.append("Enter the user to whom you want to ").append(wordReplace)
                    .append(" access to the specified command. ")
                    .append("You can specify (listed in priority order when performing ")
                    .append("a user search with a bot): user ID, its mention, discriminating name ")
                    .append("(i.e. global\\_username#user\\_tag), global username ")
                    .append("(i.e. without #user\\_tag), nickname on this server.");
            case TYPE_ROLE -> descriptionText.append("Enter the role that you want to ").append(wordReplace)
                    .append(" access to the specified command. ")
                    .append("You can specify (listed in order of priority when ")
                    .append("performing a search for a role by a bot): role ID, its mention, ")
                    .append("role name.");
            case TYPE_TEXT_CHAT -> {
                descriptionText.append("Enter the text channel that you want to ").append(wordReplace)
                        .append(" the execution of the specified command. ");
                if (!isAdditionRights) {
                    descriptionText.append("Keep in mind that permission to execute commands for a category ")
                            .append("of text channels override the lack of rights to execute commands ")
                            .append("for text channels in this category. ");
                }
                descriptionText.append("You can specify (listed in order of priority when ")
                        .append("performing a channel search with a bot): channel ID, its mention, ")
                        .append("channel name.");
            }
            case TYPE_CATEGORY -> {
                descriptionText.append("Enter the channel category in which you want to ").append(wordReplace)
                        .append(" the execution of the specified command. ");
                if (isAdditionRights) {
                    descriptionText.append("Keep in mind that the permission applies ")
                            .append("to all channels in this category (it is impossible to selectively ")
                            .append("prohibit the execution of a command on any channel in this ")
                            .append("category while the permission is valid for the entire category). ");
                } else {
                    descriptionText.append("Keep in mind that permission to execute commands for a category ")
                            .append("of text channels override the lack of rights to execute commands ")
                            .append("for text channels in this category. ");
                }
                descriptionText.append("You can specify (listed in order of priority when performing a channel ")
                        .append("search with a bot): category ID, category name.");
            }
        }

        descriptionText.append("\nEntering a value is not case sensitive. ")
                .append("If there are two of the same name, the first one will be selected. ")
                .append("You can list several at once, on each line individually. ")
                .append("Use `Shift`+`Enter` to jump to a new line.")
                .append("\nYou can also click on ").append(EMOJI_ARROW_LEFT)
                .append(" to return to return to the previous menu and ")
                .append("click on ").append(EMOJI_CLOSE).append(" to close the editor.");
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
        Scenario.all().stream()
                .filter(ACLCommand::isNotExpertCommand)
                .forEach(scenario ->
                        descriptionText.append('`').append(scenario.getPrefix()).append('`')
                                .append(" - ").append(scenario.getCommandDescription()).append('\n'));
        BotCommand.all().stream()
                .filter(ACLCommand::isNotExpertCommand)
                .forEach(botCommand ->
                        descriptionText.append('`').append(botCommand.getPrefix()).append('`')
                                .append(" - ").append(botCommand.getCommandDescription()).append('\n'));
        descriptionText.append("__Bot reactions:__\n");
        MsgCreateReaction.all().stream()
                .filter(MsgCreateReaction::isAccessControl)
                .forEachOrdered(msgCreateReaction ->
                        descriptionText.append('`').append(msgCreateReaction.getCommandPrefix()).append('`')
                                .append(" - ").append(msgCreateReaction.getCommandDescription()).append('\n'));
        descriptionText.append("__Bot expert commands:__\n");
        Scenario.all().stream()
                .filter(ACLCommand::isExpertCommand)
                .forEach(scenario ->
                        descriptionText.append('`').append(scenario.getPrefix()).append('`')
                                .append(" - ").append(scenario.getCommandDescription()).append('\n'));
        BotCommand.all().stream()
                .filter(ACLCommand::isExpertCommand)
                .forEach(botCommand ->
                        descriptionText.append('`').append(botCommand.getPrefix()).append('`')
                                .append(" - ").append(botCommand.getCommandDescription()).append('\n'));
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
        BroadCast.sendServiceMessage("Exec of " + RightsScenario.class.getName()
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
        BroadCast.sendServiceMessage("Exec of " + RightsScenario.class.getName()
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
        BroadCast.sendServiceMessage("Exec of " + RightsScenario.class.getName()
                + "::privateMessageStep!");
        return true;
    }

    private static class ShortRightsInfo {

        final boolean hasUsers;
        final boolean hasRoles;
        final boolean hasTextChannels;
        final boolean hasChannelCategories;
        final boolean isThereAnything;

        ShortRightsInfo(@NotNull Server server,
                        @NotNull String enteredCommandName) {
            CommandRights commandRights = SettingsController.getInstance()
                    .getServerPreferences(server.getId())
                    .getRightsForCommand(enteredCommandName);
            this.hasUsers = !commandRights.getAllowUsers().isEmpty()
                    && commandRights.getAllowUsers().stream()
                    .map(server::getMemberById)
                    .anyMatch(Optional::isPresent);
            this.hasRoles = !commandRights.getAllowRoles().isEmpty()
                    && commandRights.getAllowRoles().stream()
                    .map(server::getRoleById)
                    .anyMatch(Optional::isPresent);
            this.hasTextChannels = !commandRights.getAllowChannels().isEmpty()
                    && commandRights.getAllowChannels().stream()
                    .map(server::getTextChannelById)
                    .anyMatch(Optional::isPresent);
            this.hasChannelCategories = !commandRights.getAllowChannels().isEmpty()
                    && commandRights.getAllowChannels().stream()
                    .map(server::getChannelCategoryById)
                    .anyMatch(Optional::isPresent);
            this.isThereAnything = this.hasUsers
                    || this.hasRoles
                    || this.hasTextChannels
                    || this.hasChannelCategories;
        }
    }
}

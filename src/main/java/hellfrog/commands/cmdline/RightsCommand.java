package hellfrog.commands.cmdline;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.common.LongEmbedMessage;
import hellfrog.core.AccessControlCheck;
import hellfrog.core.NameCacheService;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RightsCommand
        extends BotCommand {

    private static final String PREF = "rig";
    private static final String DESCRIPTION = "Show or change command rights";
    private static final String FOOTER = "This is an expert command. To invoke an interactive user-friendly " +
            "command, use \"rights\" command. " +
            "Only server owner and users with permission " +
            "to manage server can use any command by default. Certain commands " +
            "(such as: vote) also may require permission to manage channel, " +
            "designated for a poll.";

    private static final String SPEAKER_EMOJI = EmojiParser.parseToUnicode(":loud_sound:");
    private static final String CATEGORY_EMOJI = "`v`";

    private final Option userOption = Option.builder("u")
            .longOpt("user")
            .hasArgs()
            .valueSeparator(',')
            .argName("User")
            .desc("Select user for rights control")
            .build();

    private final Option roleOption = Option.builder("r")
            .longOpt("role")
            .hasArgs()
            .valueSeparator(',')
            .argName("Role")
            .desc("Select role for rights control")
            .build();

    private final Option commandOption = Option.builder("c")
            .longOpt("command")
            .hasArgs()
            .valueSeparator(',')
            .argName("Bot command")
            .desc("Select bot command for rights control (required)")
            .build();

    private final Option allowOption = Option.builder("a")
            .longOpt("allow")
            .desc("Allow it command for this roles or users")
            .build();

    private final Option disallowOption = Option.builder("d")
            .longOpt("deny")
            .desc("Deny it command for this roles or users")
            .build();

    private final Option showAllowOption = Option.builder("s")
            .longOpt("show")
            .desc("Show roles and users that allow to execute this command (default action)")
            .build();

    private final Option channelOption = Option.builder("t")
            .longOpt("channel")
            .hasArgs()
            .optionalArg(true)
            .argName("Channel")
            .desc("Select text channel for rights control. If not arguments - use this text channel.")
            .build();

    private final Option aclModeSwitchOption = Option.builder("m")
            .longOpt("mode")
            .hasArg()
            .optionalArg(true)
            .argName("[old|new]")
            .desc("Switch access control mode for commands that has restrictions by channel" +
                    " - old and new. Old mode strongly required permissions for manage channel " +
                    "and strongly allowed role/user list. New mode strongly required permissions " +
                    "only for manage channel, allowed role/user list is optional. Default action is " +
                    "show current mode. Default mode is old.")
            .build();

    public RightsCommand() {
        super(PREF, DESCRIPTION);
        addCmdlineOption(userOption, roleOption, commandOption, allowOption, disallowOption, showAllowOption,
                channelOption, aclModeSwitchOption);
        super.enableOnlyServerCommandStrict();
        super.setFooter(FOOTER);
        super.setCommandAsExpert();
    }

    /**
     * Обработка команды, поступившей из текстового канала сервера.
     *
     * @param server      Сервер текстового канала, откуда поступило сообщение
     * @param cmdline     обработанные аргументы командной строки
     * @param cmdlineArgs оставшиеся значения командной строки
     * @param channel     текстовый канал, откуда поступило сообщение
     * @param event       событие сообщения
     */
    @Override
    protected void executeCreateMessageEventServer(Server server,
                                                   CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {

        if (!canExecuteServerCommand(event, server)) {
            showAccessDeniedServerMessage(event);
            return;
        }

        AccessControlCheck.checkAndFixAcl(server);

        boolean aclSwitchMode = cmdline.hasOption(aclModeSwitchOption.getOpt());
        String aclSwitchModeValue = cmdline.getOptionValue(aclModeSwitchOption.getOpt(), "").toLowerCase();

        if (aclSwitchMode) {
            ServerPreferencesDAO serverPreferencesDAO = SettingsController.getInstance()
                    .getMainDBController().getServerPreferencesDAO();
            if (CommonUtils.isTrStringEmpty(aclSwitchModeValue)) {
                String message = "Current access control mode is " + (serverPreferencesDAO.isNewAclMode(server.getId())
                        ? "new" : "old") + '.';
                showInfoMessage(message, event);
            } else {
                switch (aclSwitchModeValue) {
                    case "old" -> serverPreferencesDAO.setNewAclMode(server.getId(), false);
                    case "new" -> serverPreferencesDAO.setNewAclMode(server.getId(), true);
                    default -> {
                        showErrorMessage("Unknown mode. Must be \"old\" or \"new\" or " +
                                "empty for display current mode", event);
                        return;
                    }
                }

                String message = "Access control switched to " + (serverPreferencesDAO.isNewAclMode(server.getId()) ?
                        "new" : "old") + " mode.";
                showInfoMessage(message, event);
            }
            return;
        }

        if (!cmdline.hasOption(commandOption.getOpt())) {
            showErrorMessage("You have not specified a command for configuring rights.", event);
            return;
        }

        String[] commands = cmdline.getOptionValues(commandOption.getOpt());
        for (int i = 0; i < commands.length; i++)
            commands[i] = commands[i].toLowerCase();

        String unknownCommandsString = Arrays.stream(commands)
                .filter(command -> !AccessControlCheck.getAllCommandPrefix().contains(command))
                .map(command -> ServerSideResolver.getReadableContent(command, Optional.of(server)))
                .reduce(CommonUtils::reduceConcat)
                .orElse("");

        if (CommonUtils.isTrStringNotEmpty(unknownCommandsString)) {
            showErrorMessage("These commands are not recognized: " + unknownCommandsString, event);
            return;
        }

        boolean allowMode = cmdline.hasOption(allowOption.getOpt());
        boolean disallowMode = cmdline.hasOption(disallowOption.getOpt());
        boolean showMode = cmdline.hasOption(showAllowOption.getOpt());

        if (allowMode && disallowMode) {
            showErrorMessage("Cannot specify allow and deny parameters at the same time", event);
            return;
        }

        if (showMode && (allowMode || disallowMode)) {
            showErrorMessage("Cannot specify changing and display parameters at the same time.", event);
            return;
        }

        if (allowMode || disallowMode) {
            if (!canExecuteServerCommand(event, server)) {
                showAccessDeniedServerMessage(event);
                return;
            }
            boolean userModify = cmdline.hasOption(userOption.getOpt());
            List<String> usersList = CommonUtils.nullableArrayToNonNullList(cmdline.getOptionValues(userOption.getOpt()));

            boolean rolesModify = cmdline.hasOption(roleOption.getOpt());
            List<String> rolesList = CommonUtils.nullableArrayToNonNullList(cmdline.getOptionValues(roleOption.getOpt()));

            boolean channelsModify = cmdline.hasOption(channelOption.getOpt());
            List<String> channelsList = CommonUtils.nullableArrayToNonNullList(cmdline.getOptionValues(channelOption.getOpt()));
            boolean modifyThisChannel = channelsList.isEmpty();

            if (!userModify && !rolesModify && !channelsModify && !modifyThisChannel) {
                showErrorMessage("You must specify the role, user, or text channel for which you are setting rights.",
                        event);
                return;
            }

            ServerSideResolver.ParseResult<User> parsedUsers = ServerSideResolver.emptyResult();
            ServerSideResolver.ParseResult<Role> parsedRoles = ServerSideResolver.emptyResult();
            ServerSideResolver.ParseResult<ServerChannel> parsedChannels = ServerSideResolver.emptyResult();

            if (userModify) {
                parsedUsers = ServerSideResolver.resolveUsersList(server, usersList);
            }
            if (rolesModify) {
                parsedRoles = ServerSideResolver.resolveRolesList(server, rolesList);
            }
            if (channelsModify) {
                parsedChannels = ServerSideResolver.resolveAnyServerChannelList(server, channelsList);
                if (modifyThisChannel && (channel instanceof ServerTextChannel)) {
                    ServerTextChannel itsChannel = (ServerTextChannel) channel;
                    parsedChannels.getFound().add(itsChannel);
                }
            }

            LongEmbedMessage resultMessage = LongEmbedMessage.withTitleInfoStyle("Command rights");

            if (parsedUsers.hasNotFound())
                resultMessage.append("This users cannot be resolve: ")
                        .append(parsedUsers.getNotFoundStringList())
                        .appendNewLine();

            if (parsedRoles.hasNotFound())
                resultMessage.append("This roles cannot be resolve: ")
                        .append(parsedRoles.getNotFoundStringList())
                        .appendNewLine();

            if (parsedChannels.hasNotFound())
                resultMessage.append("This channels or categories cannot be resolve: ")
                        .append(parsedChannels.getNotFoundStringList())
                        .appendNewLine();

            List<User> usersChanged = new ArrayList<>();
            List<User> usersNoChanged = new ArrayList<>();

            List<Role> rolesChanged = new ArrayList<>();
            List<Role> rolesNoChanged = new ArrayList<>();

            List<ServerChannel> channelsChanged = new ArrayList<>();
            List<ServerChannel> channelsNoChanged = new ArrayList<>();

            List<ChannelCategory> categoriesChanges = new ArrayList<>();
            List<ChannelCategory> categoriesNoChanges = new ArrayList<>();

            MainDBController mainDBController = SettingsController.getInstance().getMainDBController();
            UserRightsDAO userRightsDAO = mainDBController.getUserRightsDAO();
            RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
            ChannelRightsDAO channelRightsDAO = mainDBController.getChannelRightsDAO();
            ChannelCategoryRightsDAO categoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();

            for (String command : commands) {
                for (User user : parsedUsers.getFound()) {
                    modifyRights(allowMode, userRightsDAO, server, user, command, usersChanged, usersNoChanged);
                }

                for (Role role : parsedRoles.getFound()) {
                    modifyRights(allowMode, roleRightsDAO, server, role, command, rolesChanged, rolesNoChanged);
                }

                for (ServerChannel rChannel : parsedChannels.getFound()) {
                    if (rChannel instanceof ChannelCategory) {
                        modifyRights(allowMode, categoryRightsDAO, server, (ChannelCategory) rChannel, command, categoriesChanges, categoriesNoChanges);
                    } else {
                        modifyRights(allowMode, channelRightsDAO, server, rChannel, command, channelsChanged, channelsNoChanged);
                    }
                }
            }

            if (!usersChanged.isEmpty() || !rolesChanged.isEmpty() || !channelsChanged.isEmpty() || !categoriesChanges.isEmpty()) {
                Arrays.stream(commands)
                        .reduce((c1, c2) -> c1 + ", " + c2)
                        .ifPresent(cmdlist -> resultMessage
                                .append((allowMode ? "Granted " : "Deny "), MessageDecoration.BOLD)
                                .append("rights to execute commands: ")
                                .append(cmdlist)
                                .appendNewLine());
            }

            addMentionableListToMessage(resultMessage, usersChanged, "To users:");
            addMentionableListToMessage(resultMessage, rolesChanged, "To roles:");
            addChannelsListToMessage(resultMessage, channelsChanged, "To channels:");
            addChannelsListToMessage(resultMessage, categoriesChanges, "To channel categories (and all it's channels):");

            if (!usersNoChanged.isEmpty() || !rolesNoChanged.isEmpty() || !channelsNoChanged.isEmpty() || !categoriesNoChanges.isEmpty()) {
                resultMessage.append("Settings ")
                        .append("no changed", MessageDecoration.BOLD)
                        .append(" for (already granted or cannot be granted):")
                        .appendNewLine();
                addMentionableListToMessage(resultMessage, usersNoChanged, "To users:");
                addMentionableListToMessage(resultMessage, rolesNoChanged, "To roles:");
                addChannelsListToMessage(resultMessage, channelsNoChanged, "To channels:");
                addChannelsListToMessage(resultMessage, categoriesNoChanges, "To channel categories:");
            }

            showMessage(resultMessage, event);
        } else if (showMode) {
            LongEmbedMessage resultMessage = LongEmbedMessage.withTitleInfoStyle("Command rights");

            MainDBController mainDBController = SettingsController.getInstance().getMainDBController();
            UserRightsDAO userRightsDAO = mainDBController.getUserRightsDAO();
            RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
            ChannelRightsDAO channelRightsDAO = mainDBController.getChannelRightsDAO();
            ChannelCategoryRightsDAO categoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();
            NameCacheService cacheService = SettingsController.getInstance().getNameCacheService();

            for (String cmd : commands) {
                resultMessage.append("Command: ")
                        .append(cmd, MessageDecoration.BOLD)
                        .append(":")
                        .appendNewLine();

                Optional<String> allowedUsers = userRightsDAO.getAllAllowed(server.getId(), cmd).stream()
                        .map(userId -> getLastKnownUser(cacheService, server, userId))
                        .reduce(CommonUtils::reduceNewLine);
                allowedUsers.ifPresent(usersList -> resultMessage.append("Allowed for users:", MessageDecoration.UNDERLINE)
                        .appendNewLine()
                        .append(usersList)
                        .appendNewLine());

                Optional<String> allowedRoles = roleRightsDAO.getAllAllowed(server.getId(), cmd).stream().map(roleId -> {
                    Optional<Role> mayBeRole = server.getRoleById(roleId);
                    if (mayBeRole.isPresent()) {
                        return mayBeRole.get().getMentionTag();
                    } else {
                        roleRightsDAO.deny(server.getId(), roleId, cmd);
                        return null;
                    }
                })
                        .filter(Objects::nonNull)
                        .reduce(CommonUtils::reduceNewLine);

                allowedRoles.ifPresent(rolesList -> resultMessage.append("Allowed for roles:", MessageDecoration.UNDERLINE)
                        .appendNewLine()
                        .append(rolesList)
                        .appendNewLine());

                Optional<String> allowedChannels = channelRightsDAO.getAllAllowed(server.getId(), cmd).stream().map(channelId -> {
                    Optional<ServerChannel> mayBeChannel = server.getChannelById(channelId);
                    if (mayBeChannel.isEmpty()) {
                        channelRightsDAO.deny(server.getId(), channelId, cmd);
                    } else {
                        ServerChannel serverChannel = mayBeChannel.get();
                        if (serverChannel instanceof ChannelCategory) {
                            channelRightsDAO.deny(server.getId(), channelId, cmd);
                        } else {
                            return printServerChannel(serverChannel);
                        }
                    }
                    return null;
                })
                        .filter(Objects::nonNull)
                        .reduce(CommonUtils::reduceNewLine);

                allowedChannels.ifPresent(channelsList -> resultMessage.append("Allowed for channels:", MessageDecoration.UNDERLINE)
                        .appendNewLine()
                        .append(channelsList)
                        .appendNewLine());

                Optional<String> allowedCategories = categoryRightsDAO.getAllAllowed(server.getId(), cmd).stream().map(categoryId -> {
                    Optional<ChannelCategory> mayBeCategory = server.getChannelCategoryById(categoryId);
                    if (mayBeCategory.isEmpty()) {
                        categoryRightsDAO.deny(server.getId(), categoryId, cmd);
                    } else {
                        return printServerChannel(mayBeCategory.get());
                    }
                    return null;
                })
                        .filter(Objects::nonNull)
                        .reduce(CommonUtils::reduceNewLine);

                allowedCategories.ifPresent(categoryList -> resultMessage.append("Allowed for categories (and all it's channels):", MessageDecoration.UNDERLINE)
                        .appendNewLine()
                        .append(categoryList)
                        .appendNewLine());

                if (allowedUsers.isEmpty() &&
                        allowedRoles.isEmpty() &&
                        allowedChannels.isEmpty() &&
                        allowedCategories.isEmpty())
                    resultMessage.append("No rights were specified for the command.");

                showMessage(resultMessage, event);
            }
        }
    }

    private <T extends DiscordEntity> void modifyRights(final boolean allowMode,
                                                        @NotNull final RightsDAO rightsDAO,
                                                        @NotNull final Server server,
                                                        @NotNull final T entity,
                                                        @NotNull final String command,
                                                        @NotNull final List<T> changedList,
                                                        @NotNull final List<T> notChangedList) {

        boolean result = allowMode ? rightsDAO.allow(server.getId(), entity.getId(), command)
                : rightsDAO.deny(server.getId(), entity.getId(), command);
        if (result) {
            if (!changedList.contains(entity)) changedList.add(entity);
        } else {
            if (!notChangedList.contains(entity)) notChangedList.add(entity);
        }
    }

    private <T extends Mentionable> void addMentionableListToMessage(@NotNull final LongEmbedMessage resultMessage,
                                                                     @NotNull final List<T> entities,
                                                                     @NotNull final String header) {
        entities.stream()
                .map(Mentionable::getMentionTag)
                .reduce(CommonUtils::reduceNewLine)
                .ifPresent(line ->
                        resultMessage.append(header)
                                .appendNewLine()
                                .append(line)
                                .appendNewLine());
    }

    private <T extends ServerChannel> void addChannelsListToMessage(@NotNull final LongEmbedMessage resultMessage,
                                                                    @NotNull final List<T> entities,
                                                                    @NotNull final String header) {
        entities.stream()
                .map(this::printServerChannel)
                .reduce(CommonUtils::reduceNewLine)
                .ifPresent(line ->
                        resultMessage.append(header)
                                .appendNewLine()
                                .append(line)
                                .appendNewLine());
    }

    private <T extends ServerChannel> String printServerChannel(@NotNull final T serverChannel) {
        if (serverChannel instanceof ServerTextChannel) {
            return ((ServerTextChannel) serverChannel).getMentionTag();
        } else {
            final String name = ServerSideResolver.getReadableContent(serverChannel.getName(), Optional.of(serverChannel.getServer()));
            if (serverChannel instanceof ServerVoiceChannel) {
                return SPEAKER_EMOJI + name;
            } else if (serverChannel instanceof ChannelCategory) {
                return CATEGORY_EMOJI + name;
            } else {
                return "#" + name;
            }
        }
    }

    private String getLastKnownUser(@NotNull final NameCacheService cacheService,
                                    @NotNull final Server server,
                                    final long userId) {

        Optional<User> mayBeMember = server.getMemberById(userId);
        if (mayBeMember.isPresent()) {
            return mayBeMember.get().getMentionTag();
        }
        final StringBuilder result = new StringBuilder()
                .append("(member not present on server) ");
        cacheService.findLastKnownName(server, userId).ifPresent(lastKnownNick ->
                result.append("Nickname: ").append(lastKnownNick).append(", "));
        boolean foundDiscriminatedName = false;
        try {
            User user = server.getApi().getUserById(userId).get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
            if (user != null && !cacheService.isDeletedUserDiscriminatedName(user.getDiscriminatedName())) {
                cacheService.update(user);
                result.append(ServerSideResolver.getReadableContent(user.getDiscriminatedName(), Optional.of(server))).append(", ");
                foundDiscriminatedName = true;
            }
        } catch (Exception ignore) {
        }
        if (!foundDiscriminatedName) {
            cacheService.findLastKnownName(userId).ifPresentOrElse(cachedName ->
                            result.append(ServerSideResolver.getReadableContent(cachedName, Optional.of(server))).append(", "),
                    () -> result.append("[discriminated name is unknown], "));
        }
        result.append("ID: ").append(userId);
        return result.toString();
    }

    /**
     * Обработка команды, поступившей из привата.
     *
     * @param cmdline     обработанные аргументы командной строки
     * @param cmdlineArgs оставшиеся значения командной строки
     * @param channel     текстовый канал, откуда поступило сообщение
     * @param event       событие сообщения
     */
    @Override
    protected void executeCreateMessageEventDirect(CommandLine cmdline, ArrayList<String> cmdlineArgs,
                                                   TextChannel channel, MessageCreateEvent event,
                                                   ArrayList<String> anotherLines) {
        // директы поступать не будут, ограничено
    }
}

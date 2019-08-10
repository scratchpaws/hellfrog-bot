package hellfrog.commands.cmdline;

import hellfrog.common.CommonUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.CommandRights;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class RightsCommand
        extends BotCommand {

    private static final String PREF = "rig";
    private static final String DESCRIPTION = "Show or change command rights";
    private static final String FOOTER = "Only server owner and users with permission " +
            "to manage server can use any command by default. Certain commands " +
            "(such as: vote) also may require permission to manage channel, " +
            "designated for a poll.";

    public RightsCommand() {
        super(PREF, DESCRIPTION);

        Option userOption = Option.builder("u")
                .longOpt("user")
                .hasArgs()
                .valueSeparator(',')
                .argName("User")
                .desc("Select user for rights control")
                .build();

        Option roleOption = Option.builder("r")
                .longOpt("role")
                .hasArgs()
                .valueSeparator(',')
                .argName("Role")
                .desc("Select role for rights control")
                .build();

        Option commandOption = Option.builder("c")
                .longOpt("command")
                .hasArgs()
                .valueSeparator(',')
                .argName("Bot command")
                .desc("Select bot command for rights control (required)")
                .build();

        Option allowOption = Option.builder("a")
                .longOpt("allow")
                .desc("Allow it command for this roles or users")
                .build();

        Option disallowOption = Option.builder("d")
                .longOpt("deny")
                .desc("Deny it command for this roles or users")
                .build();

        Option showAllowOption = Option.builder("s")
                .longOpt("show")
                .desc("Show roles and users that allow to execute this command (default action)")
                .build();

        Option channelOption = Option.builder("t")
                .longOpt("channel")
                .hasArgs()
                .optionalArg(true)
                .argName("Channel")
                .desc("Select text channel for rights control. If not arguments - use this text channel.")
                .build();

        Option aclModeSwitchOption = Option.builder("m")
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

        addCmdlineOption(userOption, roleOption, commandOption, allowOption, disallowOption, showAllowOption,
                channelOption, aclModeSwitchOption);

        super.enableOnlyServerCommandStrict();
        super.setFooter(FOOTER);
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

        SettingsController settingsController = SettingsController.getInstance();

        boolean aclSwitchMode = cmdline.hasOption('m');
        String aclSwitchModeValue = cmdline.getOptionValue('m', "").toLowerCase();

        if (aclSwitchMode) {
            ServerPreferences serverPreferences = settingsController.getServerPreferences(server.getId());
            if (CommonUtils.isTrStringEmpty(aclSwitchModeValue)) {
                String message = "Current access control mode is " + (serverPreferences.getNewAclMode() ?
                        "new" : "old") + '.';
                showInfoMessage(message, event);
            } else {
                if (!canExecuteServerCommand(event, server)) {
                    showAccessDeniedServerMessage(event);
                    return;
                }
                switch (aclSwitchModeValue) {
                    case "old":
                        serverPreferences.setNewAclMode(false);
                        break;

                    case "new":
                        serverPreferences.setNewAclMode(true);
                        break;

                    default:
                        showErrorMessage("Unknown mode. Must be \"old\" or \"new\" or " +
                                "empty for display current mode", event);
                        return;
                }

                settingsController.saveServerSideParameters(server.getId());

                String message = "Access control switched to " + (serverPreferences.getNewAclMode() ?
                        "new" : "old") + " mode.";
                showInfoMessage(message, event);
            }

            return;
        }

        if (!cmdline.hasOption('c')) {
            showErrorMessage("You have not specified a command for configuring rights.", event);
            return;
        }

        String[] commands = cmdline.getOptionValues('c');
        for (int i = 0; i < commands.length; i++)
            commands[i] = commands[i].toLowerCase();

        Map<String, MsgCreateReaction> knownReactionsWithAcl = new HashMap<>(); //// AAAAAAA why map?
        MsgCreateReaction.all().stream()
                .filter(MsgCreateReaction::isAccessControl)
                .forEach(r -> knownReactionsWithAcl.put(r.getCommandPrefix(), r));

        List<String> unknownCommands = new ArrayList<>(commands.length);// todo AAAAAAAA
        for (String cmd : commands) {
            if (BotCommand.all().stream().noneMatch(c -> c.getPrefix().equals(cmd))
                    && !knownReactionsWithAcl.containsKey(cmd))
                unknownCommands.add(cmd);
        }

        if (unknownCommands.size() > 0) {
            unknownCommands.stream()
                    .reduce((cmd1, cmd2) -> cmd1 + ", " + cmd2)
                    .ifPresent(res -> showErrorMessage("These commands are not recognized:" + res, event));
            return;
        }

        boolean allowMode = cmdline.hasOption('a');
        boolean disallowMode = cmdline.hasOption('d');
        boolean showMode = cmdline.hasOption('s');

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
            boolean userModify = cmdline.hasOption('u');
            List<String> usersList = CommonUtils.nullableArrayToNonNullList(cmdline.getOptionValues('u'));

            boolean rolesModify = cmdline.hasOption('r');
            List<String> rolesList = CommonUtils.nullableArrayToNonNullList(cmdline.getOptionValues('r'));

            boolean channelsModify = cmdline.hasOption('t');
            List<String> channelsList = CommonUtils.nullableArrayToNonNullList(cmdline.getOptionValues('t'));
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
                parsedChannels = ServerSideResolver.resolveTextChannelsAndCategoriesList(server, channelsList);
                if (modifyThisChannel && (channel instanceof ServerTextChannel)) {
                    ServerTextChannel itsChannel = (ServerTextChannel) channel;
                    parsedChannels.getFound().add(itsChannel);
                }
            }

            MessageBuilder resultMessage = new MessageBuilder();

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

            List<CommandRights> commandRightsList = Arrays.stream(commands)
                    .map(cmd -> settingsController.getServerPreferences(server.getId())
                            .getRightsForCommand(cmd))
                    .collect(Collectors.toList());

            List<User> usersChanged = new ArrayList<>(parsedUsers.getFound().size());
            List<User> usersNoChanged = new ArrayList<>(usersChanged.size());

            List<Role> rolesChanged = new ArrayList<>(parsedRoles.getFound().size());
            List<Role> rolesNoChanged = new ArrayList<>(rolesChanged.size());

            List<ServerChannel> channelsChanged = new ArrayList<>(parsedChannels.getFound().size());
            List<ServerChannel> channelsNoChanged = new ArrayList<>(channelsChanged.size());

            for (User user : parsedUsers.getFound()) {
                commandRightsList.stream().map((commandRights) -> allowMode ?
                        commandRights.addAllowUser(user.getId()) :
                        commandRights.delAllowUser(user.getId())).forEachOrdered((result) -> {
                    if (result) {
                        usersChanged.add(user);
                    } else {
                        usersNoChanged.add(user);
                    }
                });
            }

            for (Role role : parsedRoles.getFound()) {
                commandRightsList.stream().map((commandRights) -> allowMode ?
                        commandRights.addAllowRole(role.getId()) :
                        commandRights.delAllowRole(role.getId())).forEachOrdered((result) -> {
                    if (result) {
                        rolesChanged.add(role);
                    } else {
                        rolesNoChanged.add(role);
                    }
                });
            }

            for (ServerChannel rChannel : parsedChannels.getFound()) {
                commandRightsList.stream().map((commandRights) -> allowMode ?
                        commandRights.addAllowChannel(rChannel.getId()) :
                        commandRights.delAllowChannel(rChannel.getId())).forEachOrdered((result) -> {
                    if (result) {
                        channelsChanged.add(rChannel);
                    } else {
                        channelsNoChanged.add(rChannel);
                    }
                });
            }

            settingsController.saveServerSideParameters(server.getId());

            if (usersChanged.size() > 0 || rolesChanged.size() > 0 || channelsChanged.size() > 0) {
                Arrays.stream(commands)
                        .reduce((c1, c2) -> c1 + ", " + c2)
                        .ifPresent(cmdlist -> resultMessage
                                .append((allowMode ? "Granted " : "Deny "), MessageDecoration.BOLD)
                                .append("rights to execute commands: ")
                                .append(cmdlist)
                                .appendNewLine());
            }

            addUsersListToMessage(resultMessage, usersChanged);
            addRolesListToMessage(resultMessage, rolesChanged);
            addServerTextChannelsListToMessage(resultMessage, channelsChanged);

            if (usersNoChanged.size() > 0 || rolesNoChanged.size() > 0 || channelsNoChanged.size() > 0) {
                resultMessage.append("Settings ")
                        .append("no changed", MessageDecoration.BOLD)
                        .append(" (already granted or cannot be granted):")
                        .appendNewLine();
                addUsersListToMessage(resultMessage, usersNoChanged);
                addRolesListToMessage(resultMessage, rolesNoChanged);
                addServerTextChannelsListToMessage(resultMessage, channelsNoChanged);
            }

            showInfoMessage(resultMessage.getStringBuilder().toString(), event);
        } else if (showMode) {
            MessageBuilder msgBuilder = new MessageBuilder();
            for (String cmd : commands) {
                msgBuilder.append("Command: ")
                        .append(cmd, MessageDecoration.BOLD)
                        .append(":")
                        .appendNewLine();
                CommandRights commandRights = settingsController.getServerPreferences(server.getId())
                        .getRightsForCommand(cmd);

                Optional<String> resolvedAllowedUsers = commandRights.getAllowUsers().stream()
                        .map(userId -> {
                            Optional<User> mayBeUser = server.getMemberById(userId);
                            if (mayBeUser.isPresent()) {
                                User user = mayBeUser.get();
                                return user.getDiscriminatedName() + " (id: " + userId + ")";
                            } else {
                                commandRights.delAllowUser(userId);
                                settingsController.saveServerSideParameters(server.getId());
                                return "[leaved-user, now removed from settings] (id: " + userId + ")";
                            }
                        }).reduce(CommonUtils::reduceConcat);
                resolvedAllowedUsers.ifPresent(s -> msgBuilder.append("  * allow for users: ")
                        .append(s)
                        .appendNewLine());

                Optional<String> resolvedAllowedRoles = commandRights.getAllowRoles().stream()
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
                resolvedAllowedRoles.ifPresent(s -> msgBuilder.append("  * allow for roles: ")
                        .append(s)
                        .appendNewLine());

                Optional<String> resolvedAllowedChannels = commandRights.getAllowChannels().stream()
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
                resolvedAllowedChannels.ifPresent(s -> msgBuilder.append("  * allow for channels: ")
                        .append(s)
                        .appendNewLine());

                Optional<String> resolvedAllowedCategories = commandRights.getAllowChannels().stream()
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
                resolvedAllowedCategories.ifPresent(s -> msgBuilder.append("  * allowed for categories: ")
                        .append(s)
                        .appendNewLine());

                if (resolvedAllowedUsers.isEmpty() &&
                        resolvedAllowedRoles.isEmpty() &&
                        resolvedAllowedChannels.isEmpty() &&
                        resolvedAllowedCategories.isEmpty())
                    msgBuilder.append("  * No rights were specified for the command.");
                showInfoMessage(msgBuilder.getStringBuilder().toString(), event);
            }
        }
    }

    private void addUsersListToMessage(MessageBuilder resultMessage, @NotNull List<User> users) {
        users.stream()
                .map(u -> u.getDiscriminatedName() + " (id: " + u.getIdAsString() + ")")
                .reduce(CommonUtils::reduceConcat)
                .ifPresent(usrlist -> resultMessage
                        .append("Users: ")
                        .append(usrlist)
                        .appendNewLine());
    }

    private void addRolesListToMessage(MessageBuilder resultMessage, @NotNull List<Role> roles) {
        roles.stream()
                .map(r -> r.getName() + " (id: " + r.getIdAsString() + ")")
                .reduce(CommonUtils::reduceConcat)
                .ifPresent(rlist -> resultMessage
                        .append("Roles: ")
                        .append(rlist)
                        .appendNewLine());
    }

    private void addServerTextChannelsListToMessage(@NotNull MessageBuilder resultMessage,
                                                    @NotNull List<ServerChannel> channels) {
        channels.stream()
                .filter(c -> c instanceof ServerTextChannel)
                .map(this::getChannelNameAndId)
                .reduce(CommonUtils::reduceConcat)
                .ifPresent(clist -> resultMessage
                        .append("For channels: ")
                        .append(clist)
                        .appendNewLine());
        channels.stream()
                .filter(c -> c instanceof ChannelCategory)
                .map(this::getChannelNameAndId)
                .reduce(CommonUtils::reduceConcat)
                .ifPresent(cliet -> resultMessage
                        .append("For categories: ")
                        .append(cliet)
                        .appendNewLine());
    }

    @NotNull
    private String getChannelNameAndId(@NotNull ServerChannel channel) {
        return channel.getName() + " (id: " + channel.getIdAsString() + ")";
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

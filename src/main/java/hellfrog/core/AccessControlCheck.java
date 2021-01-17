package hellfrog.core;

import hellfrog.commands.ACLCommand;
import hellfrog.commands.cmdline.BotCommand;
import hellfrog.commands.scenes.Scenario;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.CommandRights;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccessControlCheck {

    private static List<String> allCommandPrefix = null;
    private static final ReentrantLock prefixLoadLock = new ReentrantLock();

    public static boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull User user,
                                             @NotNull Server server, @NotNull TextChannel channel,
                                             boolean strictByChannels, long... anotherTargetChannel) {

        SettingsController settingsController = SettingsController.getInstance();

        long userId = user.getId();
        long serverId = server.getId();
        long channelId = channel.getId();

        boolean isNewAclMode = settingsController.getServerPreferences(serverId).getNewAclMode();
        CommandRights commandRights = settingsController.getServerPreferences(serverId)
                .getRightsForCommand(commandPrefix);

        boolean isServerAdmin = server.isAdmin(user) || server.canManage(user);
        boolean isAllowUser = commandRights.isAllowUser(userId);
        List<Long> userRolesIds = user.getRoles(server).stream()
                .map(Role::getId)
                .collect(Collectors.toList());
        boolean isHasAllowedRole = commandRights.getAllowRoles().stream()
                .anyMatch(userRolesIds::contains);

        boolean isAllowedForChannel = true;
        boolean allowChannelsListNotEmpty = !commandRights.getAllowChannels().isEmpty();
        boolean anotherTargetChannelsNotEmpty = anotherTargetChannel != null
                && anotherTargetChannel.length > 0;
        boolean hasAllowedRolesOrUsers = !commandRights.getAllowUsers().isEmpty()
                || !commandRights.getAllowRoles().isEmpty();

        if (strictByChannels && allowChannelsListNotEmpty) {
            if (anotherTargetChannelsNotEmpty) {
                for (long anotherChannelId : anotherTargetChannel) {
                    isAllowedForChannel &= isAllowChatOrCategory(server, commandRights, anotherChannelId);
                }
            } else {
                isAllowedForChannel = isAllowChatOrCategory(server, commandRights, channelId);
            }
        } else if (strictByChannels && isNewAclMode) {
            isAllowedForChannel = false;
        }
        return (isAllowedForChannel && (isHasAllowedRole || isAllowUser))
                // дополнение к логике ACL, new mode
                || (isNewAclMode && strictByChannels && isAllowedForChannel && !hasAllowedRolesOrUsers)
                || isServerAdmin;
    }

    private static boolean isAllowChatOrCategory(@NotNull Server server,
                                                 @NotNull CommandRights commandRights,
                                                 long channelId) {
        if (commandRights.isAllowChat(channelId)) {
            return true;
        }
        return server.getTextChannelById(channelId)
                .map(ServerTextChannel::getCategory)
                .map(mayBeCategory ->
                        mayBeCategory.map(ChannelCategory::getId)
                                .map(commandRights::isAllowChat)
                                .orElse(false)
                ).orElse(false);
    }

    public static boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull MessageCreateEvent event,
                                             @NotNull Server server,
                                             boolean strictByChannels, long... anotherTargetChannels) {
        return event.getMessageAuthor().asUser().map(user ->
                canExecuteOnServer(commandPrefix, user, server,
                        event.getChannel(), strictByChannels,
                        anotherTargetChannels)
        ).orElse(false);
    }

    public static boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull SingleReactionEvent event,
                                             @NotNull Server server,
                                             boolean strictByChannels, long... anotherTargetChannels) {
        return event.getUser().map(user ->
                canExecuteOnServer(commandPrefix, user, server,
                        event.getChannel(), strictByChannels,
                        anotherTargetChannels)
        ).orElse(false);
    }

    public static boolean canExecuteGlobalCommand(@NotNull MessageCreateEvent event) {
        long userId = event.getMessageAuthor().getId();
        long botOwner = event.getApi().getOwnerId();
        return SettingsController.getInstance().isGlobalBotOwner(userId) || userId == botOwner;
    }

    public static boolean canExecuteGlobalCommand(@NotNull SingleReactionEvent event) {
        long userId = event.getUserId();
        long botOwner = event.getApi().getOwnerId();
        return SettingsController.getInstance().isGlobalBotOwner(userId) || userId == botOwner;
    }

    public static void checkAndFixAcl(@NotNull final Server server) {
        MainDBController mainDBController = SettingsController.getInstance().getMainDBController();
        ServerPreferencesDAO serverPreferencesDAO = mainDBController.getServerPreferencesDAO();
        if (serverPreferencesDAO.isAclFixRequired(server.getId())) {
            RoleRightsDAO roleRightsDAO = mainDBController.getRoleRightsDAO();
            ChannelRightsDAO channelRightsDAO = mainDBController.getChannelRightsDAO();
            ChannelCategoryRightsDAO categoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();
            getAllCommandPrefix().forEach(prefix -> {
                roleRightsDAO.getAllAllowed(server.getId(), prefix).forEach(roleId -> {
                    if (server.getRoleById(roleId).isEmpty()) {
                        roleRightsDAO.deny(server.getId(), roleId, prefix);
                    }
                });
                channelRightsDAO.getAllAllowed(server.getId(), prefix).forEach(channelId -> {
                    Optional<ServerChannel> mayBeChannel = server.getChannelById(channelId);
                    if (mayBeChannel.isEmpty()) {
                        channelRightsDAO.deny(server.getId(), channelId, prefix);
                    } else {
                        ServerChannel serverChannel = mayBeChannel.get();
                        if (serverChannel instanceof ChannelCategory) {
                            channelRightsDAO.deny(server.getId(), channelId, prefix);
                        }
                    }
                });
                categoryRightsDAO.getAllAllowed(server.getId(), prefix).forEach(categoryId -> {
                    Optional<ChannelCategory> mayBeChannel = server.getChannelCategoryById(categoryId);
                    if (mayBeChannel.isEmpty()) {
                        channelRightsDAO.deny(server.getId(), categoryId, prefix);
                    }
                });
            });
            serverPreferencesDAO.setAclFixRequired(server.getId(), false);
        }
    }

    public static List<String> getAllCommandPrefix() {
        if (allCommandPrefix == null) {
            prefixLoadLock.lock();
            try {
                if (allCommandPrefix == null) {
                    List<String> allCommands = new ArrayList<>();
                    for (MsgCreateReaction reaction : MsgCreateReaction.all()) {
                        allCommands.add(reaction.getCommandPrefix());
                    }
                    for (BotCommand command : BotCommand.all()) {
                        allCommands.add(command.getPrefix());
                    }
                    for (Scenario scenario : Scenario.all()) {
                        allCommands.add(scenario.getPrefix());
                    }
                    Collections.sort(allCommands);
                    allCommandPrefix = Collections.unmodifiableList(allCommands);
                }
            } finally {
                prefixLoadLock.unlock();
            }
        }
        return allCommandPrefix;
    }
}

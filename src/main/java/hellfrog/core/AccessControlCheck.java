package hellfrog.core;

import hellfrog.settings.CommandRights;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class AccessControlCheck {

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
        return canExecuteOnServer(commandPrefix, event.getUser(), server,
                event.getChannel(), strictByChannels,
                anotherTargetChannels);
    }

    public static boolean canExecuteGlobalCommand(@NotNull MessageCreateEvent event) {
        long userId = event.getMessageAuthor().getId();
        long botOwner = event.getApi().getOwnerId();
        return SettingsController.getInstance().isGlobalBotOwner(userId) || userId == botOwner;
    }

    public static boolean canExecuteGlobalCommand(@NotNull SingleReactionEvent event) {
        long userId = event.getUser().getId();
        long botOwner = event.getApi().getOwnerId();
        return SettingsController.getInstance().isGlobalBotOwner(userId) || userId == botOwner;
    }
}

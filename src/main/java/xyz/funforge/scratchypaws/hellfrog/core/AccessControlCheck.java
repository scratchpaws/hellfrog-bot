package xyz.funforge.scratchypaws.hellfrog.core;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.CommandRights;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccessControlCheck {

    private static boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull User user,
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
                    isAllowedForChannel &= commandRights.isAllowChat(anotherChannelId);
                }
            } else {
                isAllowedForChannel = commandRights.isAllowChat(channelId);
            }
        } else if (strictByChannels && isNewAclMode) {
            isAllowedForChannel = false;
        }
        return (isAllowedForChannel && (isHasAllowedRole || isAllowUser))
                // дополнение к логике ACL, new mode
                || (isNewAclMode && strictByChannels && isAllowedForChannel && !hasAllowedRolesOrUsers)
                || isServerAdmin;
    }

    public static boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull MessageCreateEvent event,
                                             @NotNull Server server,
                                             boolean strictByChannels, long... anotherTargetChannel) {
        Optional<User> mayBeUser = event.getMessageAuthor().asUser();
        return mayBeUser.filter(user ->
                canExecuteOnServer(commandPrefix, user, server,
                        event.getChannel(), strictByChannels,
                        anotherTargetChannel)
        ).isPresent();
    }

    public static boolean canExecuteGlobalCommand(@NotNull MessageCreateEvent event) {
        long userId = event.getMessageAuthor().getId();
        long botOwner = event.getApi().getOwnerId();
        return SettingsController.getInstance().isGlobalBotOwner(userId) || userId == botOwner;
    }
}

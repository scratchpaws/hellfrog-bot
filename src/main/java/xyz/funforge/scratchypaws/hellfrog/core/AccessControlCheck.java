package xyz.funforge.scratchypaws.hellfrog.core;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.util.List;
import java.util.Optional;

public class AccessControlCheck {

    static boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull User user,
                                      @NotNull Server server, @NotNull TextChannel channel,
                                      boolean strictByChannels, long... anotherTargetChannel) {
        SettingsController settingsController = SettingsController.getInstance();
        long userId = user.getId();
        long serverId = server.getId();
        boolean isAllowUser = settingsController.getServerPreferences(serverId)
                .getRightsForCommand(commandPrefix)
                .isAllowUser(userId);
        boolean isBotRoleOwner = false;

        List<Long> allowRoles = settingsController.getServerPreferences(serverId)
                .getRightsForCommand(commandPrefix)
                .getAllowRoles();
        for (Role role : user.getRoles(server)) {
            if (allowRoles.contains(role.getId())) {
                isBotRoleOwner = true;
                break;
            }
        }

        boolean isServerAdmin = server.isAdmin(user) || server.canManage(user);

        boolean isAllowedForChannel = true;
        if (strictByChannels && settingsController.getServerPreferences(serverId)
                .getRightsForCommand(commandPrefix).getAllowChannels().size() > 0) {
            long channelId = channel.getId();
            if (anotherTargetChannel != null && anotherTargetChannel.length > 0) {
                for (long anotherChannelId : anotherTargetChannel) {
                    isAllowedForChannel &= settingsController.getServerPreferences(serverId)
                            .getRightsForCommand(commandPrefix)
                            .isAllowChat(anotherChannelId);
                }
            } else {
                isAllowedForChannel = settingsController.getServerPreferences(serverId)
                        .getRightsForCommand(commandPrefix)
                        .isAllowChat(channelId);
            }
        }
        return (isAllowedForChannel && (isBotRoleOwner || isAllowUser)) || isServerAdmin;
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
}

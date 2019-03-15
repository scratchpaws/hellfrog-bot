package xyz.funforge.scratchypaws.hellfrog.core;

import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;

import java.util.List;
import java.util.Optional;

public class AccessControlCheck {

    public static boolean canExecuteOnServer(String prefix, MessageCreateEvent event, Server server,
                                             boolean strictByChannels, long... anotherTargetChannel) {
        SettingsController settingsController = SettingsController.getInstance();
        MessageAuthor messageAuthor = event.getMessageAuthor();
        long userId = messageAuthor.getId();
        long serverId = server.getId();
        boolean isAllowUser = settingsController.getServerPreferences(serverId)
                .getRightsForCommand(prefix)
                .isAllowUser(userId);
        boolean isBotRoleOwner = false;
        Optional<User> mayBeUser = messageAuthor.asUser();
        if (mayBeUser.isPresent()) {
            User user = mayBeUser.get();
            List<Long> allowRoles = settingsController.getServerPreferences(serverId)
                    .getRightsForCommand(prefix)
                    .getAllowRoles();
            for (Role role : user.getRoles(server)) {
                if (allowRoles.contains(role.getId())) {
                    isBotRoleOwner = true;
                    break;
                }
            }
        }
        boolean isServerAdmin = messageAuthor.canManageServer() ||
                messageAuthor.isServerAdmin();
        boolean isAllowedForChannel = true;
        if (strictByChannels && settingsController.getServerPreferences(serverId)
                .getRightsForCommand(prefix).getAllowChannels().size() > 0) {
            long channelId = event.getChannel().getId();
            if (anotherTargetChannel != null && anotherTargetChannel.length > 0) {
                for (long anotherChannelId : anotherTargetChannel) {
                    isAllowedForChannel &= settingsController.getServerPreferences(serverId)
                            .getRightsForCommand(prefix)
                            .isAllowChat(anotherChannelId);
                }
            } else {
                isAllowedForChannel = settingsController.getServerPreferences(serverId)
                        .getRightsForCommand(prefix)
                        .isAllowChat(channelId);
            }
        }
        return (isAllowedForChannel && (isBotRoleOwner || isAllowUser)) || isServerAdmin;
    }
}

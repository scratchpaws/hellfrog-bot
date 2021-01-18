package hellfrog.core;

import hellfrog.commands.cmdline.BotCommand;
import hellfrog.commands.scenes.Scenario;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.db.*;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class AccessControlService {

    private List<String> allCommandPrefix = null;
    private final ReentrantLock prefixLoadLock = new ReentrantLock();

    private final UserRightsDAO userRightsDAO;
    private final RoleRightsDAO roleRightsDAO;
    private final ChannelRightsDAO channelRightsDAO;
    private final ChannelCategoryRightsDAO categoryRightsDAO;
    private final ServerPreferencesDAO serverPreferencesDAO;
    private final BotOwnersDAO botOwnersDAO;

    public AccessControlService(@NotNull final MainDBController mainDBController) {
        this.userRightsDAO = mainDBController.getUserRightsDAO();
        this.roleRightsDAO = mainDBController.getRoleRightsDAO();
        this.channelRightsDAO = mainDBController.getChannelRightsDAO();
        this.categoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();
        this.serverPreferencesDAO = mainDBController.getServerPreferencesDAO();
        this.botOwnersDAO = mainDBController.getBotOwnersDAO();
    }

    public boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull User user,
                                      @NotNull Server server, @NotNull ServerChannel channel,
                                      boolean strictByChannels, long... anotherTargetChannel) {

        checkAndFixAcl(server); // legacy conversion fix

        // Administrator cannot be strict
        boolean isServerAdmin = server.isAdmin(user) || server.canManage(user);
        if (isServerAdmin) {
            return true;
        }

        long userId = user.getId();
        long serverId = server.getId();
        long channelId = channel.getId();

        // Check that command is allowed for user or his one or more roles
        boolean isAllowUser = userRightsDAO.isAllowed(serverId, userId, commandPrefix);
        boolean isHasAllowedRole = user.getRoles(server).stream()
                .anyMatch(role -> roleRightsDAO.isAllowed(serverId, role.getId(), commandPrefix));

        if (!strictByChannels) {
            // No channel strict, skipping channel check
            return isAllowUser || isHasAllowedRole;
        }

        boolean isAllowedChannel;
        if (anotherTargetChannel != null && anotherTargetChannel.length > 0) {
            // If has another target channels, skip origin channel
            isAllowedChannel = true;
            for (long anotherId : anotherTargetChannel) {
                isAllowedChannel &= isAllowedServerChannel(server, commandPrefix, anotherId);
            }
        } else {
            isAllowedChannel = isAllowedServerChannel(server, commandPrefix, channelId);
        }

        if (!isAllowedChannel) {
            return false;
        }

        boolean isNewAclMode = serverPreferencesDAO.isNewAclMode(serverId);
        if (isNewAclMode) {
            // New ACL mode: required allowed channel(s). Allowed role/user is optional
            boolean hasRequiredUsersOrRoles = roleRightsDAO.getAllowedCount(serverId, commandPrefix) > 0L
                    || userRightsDAO.getAllowedCount(serverId, commandPrefix) > 0L;
            if (!hasRequiredUsersOrRoles) {
                return true;
            }
        }
        // Old ACL mode: required allowed channel(s) and allowed role/user
        return isAllowUser || isHasAllowedRole;
    }

    private boolean isAllowedServerChannel(@NotNull Server server, @NotNull String commandPrefix, long channelId) {
        if (channelRightsDAO.isAllowed(server.getId(), channelId, commandPrefix)) {
            return true;
        }

        boolean allowedTextChatCategory = server.getTextChannelById(channelId)
                .flatMap(ServerTextChannel::getCategory)
                .map(category -> isAllowedChannelCategory(category, commandPrefix))
                .orElse(false);
        boolean allowedVoiceChatCategory = server.getVoiceChannelById(channelId)
                .flatMap(ServerVoiceChannel::getCategory)
                .map(category -> isAllowedChannelCategory(category, commandPrefix))
                .orElse(false);
        return allowedTextChatCategory || allowedVoiceChatCategory;
    }

    private boolean isAllowedChannelCategory(@NotNull ChannelCategory category, @NotNull final String commandPrefix) {
        return categoryRightsDAO.isAllowed(category.getServer().getId(), category.getId(), commandPrefix);
    }

    public boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull MessageCreateEvent event,
                                      @NotNull Server server,
                                      boolean strictByChannels, long... anotherTargetChannels) {
        return event.getMessageAuthor().asUser().map(user ->
                event.getServerTextChannel().map(serverTextChannel ->
                        canExecuteOnServer(commandPrefix, user, server,
                                serverTextChannel, strictByChannels,
                                anotherTargetChannels))
                        .orElse(false)
        ).orElse(false);
    }

    public boolean canExecuteOnServer(@NotNull String commandPrefix, @NotNull SingleReactionEvent event,
                                      @NotNull Server server,
                                      boolean strictByChannels, long... anotherTargetChannels) {
        return event.getUser().map(user ->
                event.getServerTextChannel().map(serverTextChannel ->
                        canExecuteOnServer(commandPrefix, user, server,
                                serverTextChannel, strictByChannels,
                                anotherTargetChannels))
                        .orElse(false)
        ).orElse(false);
    }

    public boolean canExecuteGlobalCommand(@NotNull MessageCreateEvent event) {
        long userId = event.getMessageAuthor().getId();
        long botOwner = event.getApi().getOwnerId();
        return botOwnersDAO.isPresent(userId) || userId == botOwner;
    }

    public boolean canExecuteGlobalCommand(@NotNull SingleReactionEvent event) {
        long userId = event.getUserId();
        long botOwner = event.getApi().getOwnerId();
        return botOwnersDAO.isPresent(userId) || userId == botOwner;
    }

    public void checkAndFixAcl(@NotNull final Server server) {
        if (serverPreferencesDAO.isAclFixRequired(server.getId())) {
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

    public List<String> getAllCommandPrefix() {
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

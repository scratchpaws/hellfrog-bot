package hellfrog.core;

import hellfrog.commands.cmdline.BotCommand;
import hellfrog.commands.scenes.Scenario;
import hellfrog.common.CommonUtils;
import hellfrog.common.LongEmbedMessage;
import hellfrog.reacts.MsgCreateReaction;
import hellfrog.settings.db.*;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.MessageDecoration;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class AccessControlService {

    private List<String> allCommandPrefix = null;
    private List<String> nonAdminPrefix = null;
    private final ReentrantLock prefixLoadLock = new ReentrantLock();
    private final ReentrantLock nonAdminLoadLock = new ReentrantLock();

    private final UserRightsDAO userRightsDAO;
    private final RoleRightsDAO roleRightsDAO;
    private final ChannelRightsDAO channelRightsDAO;
    private final ChannelCategoryRightsDAO categoryRightsDAO;
    private final ServerPreferencesDAO serverPreferencesDAO;
    private final BotOwnersDAO botOwnersDAO;
    private final NameCacheService nameCacheService;

    public AccessControlService(@NotNull final MainDBController mainDBController,
                                @NotNull final NameCacheService nameCacheService) {
        this.userRightsDAO = mainDBController.getUserRightsDAO();
        this.roleRightsDAO = mainDBController.getRoleRightsDAO();
        this.channelRightsDAO = mainDBController.getChannelRightsDAO();
        this.categoryRightsDAO = mainDBController.getChannelCategoryRightsDAO();
        this.serverPreferencesDAO = mainDBController.getServerPreferencesDAO();
        this.botOwnersDAO = mainDBController.getBotOwnersDAO();
        this.nameCacheService = nameCacheService;
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

    public <T extends DiscordEntity> boolean allow(@NotNull final Server server,
                                                   @NotNull final T entity,
                                                   @NotNull final String command) {
        if (getAllCommandPrefix().contains(command)) {
            if (entity instanceof User) {
                return userRightsDAO.allow(server.getId(), entity.getId(), command);
            } else if (entity instanceof Role) {
                return roleRightsDAO.allow(server.getId(), entity.getId(), command);
            } else if (entity instanceof ChannelCategory) {
                return categoryRightsDAO.allow(server.getId(), entity.getId(), command);
            } else if (entity instanceof ServerChannel) {
                return channelRightsDAO.allow(server.getId(), entity.getId(), command);
            }
        }
        return false;
    }

    public <T extends DiscordEntity> boolean deny(@NotNull final Server server,
                                                  @NotNull final T entity,
                                                  @NotNull final String command) {
        if (getAllCommandPrefix().contains(command)) {
            if (entity instanceof User) {
                return userRightsDAO.deny(server.getId(), entity.getId(), command);
            } else if (entity instanceof Role) {
                return roleRightsDAO.deny(server.getId(), entity.getId(), command);
            } else if (entity instanceof ChannelCategory) {
                return categoryRightsDAO.deny(server.getId(), entity.getId(), command);
            } else if (entity instanceof ServerChannel) {
                return channelRightsDAO.deny(server.getId(), entity.getId(), command);
            }
        }
        return false;
    }

    public <T extends DiscordEntity> void denyAll(@NotNull final Server server,
                                                  @NotNull final T entity) {
        getAllCommandPrefix().forEach(command -> deny(server, entity, command));
    }

    public <T extends DiscordEntity> boolean isAllowed(@NotNull final Server server,
                                                       @NotNull final T entity,
                                                       @NotNull final String command) {
        if (getAllCommandPrefix().contains(command)) {
            if (entity instanceof User) {
                return userRightsDAO.isAllowed(server.getId(), entity.getId(), command);
            } else if (entity instanceof Role) {
                return roleRightsDAO.isAllowed(server.getId(), entity.getId(), command);
            } else if (entity instanceof ChannelCategory) {
                return categoryRightsDAO.isAllowed(server.getId(), entity.getId(), command);
            } else if (entity instanceof ServerChannel) {
                return channelRightsDAO.isAllowed(server.getId(), entity.getId(), command);
            }
        }
        return false;
    }

    public boolean isNewAclMode(@NotNull final Server server) {
        return serverPreferencesDAO.isNewAclMode(server.getId());
    }

    public void setNewAclMode(@NotNull final Server server, boolean isNewMode) {
        serverPreferencesDAO.setNewAclMode(server.getId(), isNewMode);
    }

    public boolean notHasAnyRights(@NotNull final Server server, @NotNull final String command) {
        return userRightsDAO.getAllowedCount(server.getId(), command) == 0L
                && roleRightsDAO.getAllowedCount(server.getId(), command) == 0L
                && channelRightsDAO.getAllowedCount(server.getId(), command) == 0L
                && categoryRightsDAO.getAllowedCount(server.getId(), command) == 0L;
    }

    public boolean noHasUsersRights(@NotNull final Server server, @NotNull final String command) {
        return userRightsDAO.getAllowedCount(server.getId(), command) == 0L;
    }

    public boolean noHasRolesRights(@NotNull final Server server, @NotNull final String command) {
        return roleRightsDAO.getAllowedCount(server.getId(), command) == 0L;
    }

    public boolean noHasChannelRights(@NotNull final Server server, @NotNull final String command) {
        return channelRightsDAO.getAllowedCount(server.getId(), command) == 0L;
    }

    public boolean noHasCategoryRights(@NotNull final Server server, @NotNull final String command) {
        return categoryRightsDAO.getAllowedCount(server.getId(), command) == 0L;
    }

    public boolean hasUsersRights(@NotNull final Server server, @NotNull final String command) {
        return userRightsDAO.getAllowedCount(server.getId(), command) > 0L;
    }

    public boolean hasRolesRights(@NotNull final Server server, @NotNull final String command) {
        return roleRightsDAO.getAllowedCount(server.getId(), command) > 0L;
    }

    public boolean hasChannelRights(@NotNull final Server server, @NotNull final String command) {
        return channelRightsDAO.getAllowedCount(server.getId(), command) > 0L;
    }

    public boolean hasCategoryRights(@NotNull final Server server, @NotNull final String command) {
        return categoryRightsDAO.getAllowedCount(server.getId(), command) > 0L;
    }

    public void checkAndFixAcl(@NotNull final Server server) {
        if (serverPreferencesDAO.isAclFixRequired(server.getId())) {
            getAllCommandPrefix().forEach(prefix -> {
                userRightsDAO.getAllAllowed(server.getId(), prefix).forEach(userId -> {
                    if (server.getMemberById(userId).isEmpty()) {
                        userRightsDAO.deny(server.getId(), userId, prefix);
                    }
                });
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
                        if (reaction.isAccessControl()) {
                            allCommands.add(reaction.getCommandPrefix());
                        }
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

    public List<String> getNonAdminCommands() {
        if (nonAdminPrefix == null) {
            nonAdminLoadLock.lock();
            try {
                if (nonAdminPrefix == null) {
                    List<String> nonAdminCommands = new ArrayList<>();
                    for (MsgCreateReaction reaction : MsgCreateReaction.all()) {
                        if (reaction.isAccessControl() && !reaction.isAdminCommand()) {
                            nonAdminCommands.add(reaction.getCommandPrefix());
                        }
                    }
                    for (BotCommand command : BotCommand.all()) {
                        if (!command.isAdminCommand()) {
                            nonAdminCommands.add(command.getPrefix());
                        }
                    }
                    for (Scenario scenario : Scenario.all()) {
                        if (!scenario.isAdminCommand()) {
                            nonAdminCommands.add(scenario.getPrefix());
                        }
                    }
                    Collections.sort(nonAdminCommands);
                    nonAdminPrefix = Collections.unmodifiableList(nonAdminCommands);
                }
            } finally {
                nonAdminLoadLock.unlock();
            }
        }
        return nonAdminPrefix;
    }

    public boolean isStrictByChannelsOnServer(@NotNull final Server server,
                                              @NotNull final String command) {

        if (getAllCommandPrefix().contains(command)) {
            for (MsgCreateReaction reaction : MsgCreateReaction.all()) {
                if (reaction.isAccessControl() && reaction.isStrictByChannel()) {
                    return true;
                }
            }
            for (BotCommand botCommand : BotCommand.all()) {
                if (botCommand.isStrictByChannelsOnServer(server)) {
                    return true;
                }
            }
            for (Scenario scenario : Scenario.all()) {
                if (scenario.isStrictByChannelsOnServer(server)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean appendCommandRightsInfo(@NotNull final Server server,
                                           @NotNull final String command,
                                           @NotNull final LongEmbedMessage message) {

        Optional<String> allowedUsers = userRightsDAO.getAllAllowed(server.getId(), command).stream().map(userId -> {
            Optional<User> mayBeUser = server.getMemberById(userId);
            if (mayBeUser.isPresent()) {
                return nameCacheService.printEntityDetailed(mayBeUser.get(), server);
            } else {
                userRightsDAO.deny(server.getId(), userId, command);
                return null;
            }
        })
                .filter(Objects::nonNull)
                .reduce(CommonUtils::reduceNewLine);

        Optional<String> allowedRoles = roleRightsDAO.getAllAllowed(server.getId(), command).stream().map(roleId -> {
            Optional<Role> mayBeRole = server.getRoleById(roleId);
            if (mayBeRole.isPresent()) {
                return nameCacheService.printEntityDetailed(mayBeRole.get(), server);
            } else {
                roleRightsDAO.deny(server.getId(), roleId, command);
                return null;
            }
        })
                .filter(Objects::nonNull)
                .reduce(CommonUtils::reduceNewLine);

        Optional<String> allowedChannels = channelRightsDAO.getAllAllowed(server.getId(), command).stream().map(channelId -> {
            Optional<ServerChannel> mayBeChannel = server.getChannelById(channelId);
            if (mayBeChannel.isEmpty()) {
                channelRightsDAO.deny(server.getId(), channelId, command);
            } else {
                ServerChannel serverChannel = mayBeChannel.get();
                if (serverChannel instanceof ChannelCategory) {
                    channelRightsDAO.deny(server.getId(), channelId, command);
                } else {
                    return nameCacheService.printEntityDetailed(serverChannel, server);
                }
            }
            return null;
        })
                .filter(Objects::nonNull)
                .reduce(CommonUtils::reduceNewLine);

        Optional<String> allowedCategories = categoryRightsDAO.getAllAllowed(server.getId(), command).stream().map(categoryId -> {
            Optional<ChannelCategory> mayBeCategory = server.getChannelCategoryById(categoryId);
            if (mayBeCategory.isEmpty()) {
                categoryRightsDAO.deny(server.getId(), categoryId, command);
            } else {
                return nameCacheService.printEntityDetailed(mayBeCategory.get(), server);
            }
            return null;
        })
                .filter(Objects::nonNull)
                .reduce(CommonUtils::reduceNewLine);

        boolean hasNoRights = allowedUsers.isEmpty() &&
                allowedRoles.isEmpty() &&
                allowedChannels.isEmpty() &&
                allowedCategories.isEmpty();

        if (hasNoRights) {
            return true;
        }

        message.append("Command: ")
                .append(command, MessageDecoration.UNDERLINE, MessageDecoration.BOLD)
                .append(":")
                .appendNewLine();

        allowedUsers.ifPresent(usersList -> message.append("  * Allowed for users:")
                .appendNewLine()
                .append(usersList)
                .appendNewLine());

        allowedRoles.ifPresent(rolesList ->
                message.append("  * Allowed for roles:", MessageDecoration.UNDERLINE)
                        .appendNewLine()
                        .append(rolesList)
                        .appendNewLine());

        allowedChannels.ifPresent(channelsList ->
                message.append("  * Allowed for channels:", MessageDecoration.UNDERLINE)
                        .appendNewLine()
                        .append(channelsList)
                        .appendNewLine());

        allowedCategories.ifPresent(categoryList ->
                message.append("  * Allowed for categories (and all it's channels):", MessageDecoration.UNDERLINE)
                        .appendNewLine()
                        .append(categoryList)
                        .appendNewLine());

        return false;
    }
}

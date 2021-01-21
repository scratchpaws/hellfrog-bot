package hellfrog.core;

import com.vdurmont.emoji.EmojiParser;
import hellfrog.common.CommonConstants;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.EntityNameCacheDAO;
import hellfrog.settings.db.entity.EntityNameCache;
import hellfrog.settings.db.entity.NameType;
import hellfrog.settings.db.entity.ServerNameCache;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.CertainMessageEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class NameCacheService
        implements Runnable {

    private final ScheduledFuture<?> scheduled;
    private final EntityNameCacheDAO entityNameCacheDAO;
    private final SettingsController settingsController;
    private final Pattern DELETER_USERS_PATTERN = Pattern.compile("^Deleted User(#0{4}| [a-h0-9]*#\\d{4})");

    private static final String SPEAKER_EMOJI = EmojiParser.parseToUnicode(":loud_sound:");
    private static final String CATEGORY_EMOJI = "`v`";

    public NameCacheService(@NotNull final SettingsController settingsController,
                            @NotNull final EntityNameCacheDAO entityNameCacheDAO) {
        this.settingsController = settingsController;
        this.entityNameCacheDAO = entityNameCacheDAO;
        scheduled = Executors.newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(this, 10L, 10L, TimeUnit.MINUTES);
    }

    public boolean isDeletedUserDiscriminatedName(@NotNull String discriminatedName) {
        return DELETER_USERS_PATTERN.matcher(discriminatedName).find();
    }

    public void update(@Nullable Server server) {
        if (server != null) {
            entityNameCacheDAO.update(server.getId(), server.getName(), NameType.SERVER);
            entityNameCacheDAO.update(server.getId(), server.getId(), server.getName());
        }
    }

    public void update(@Nullable User user) {
        if (user != null) {
            if (isDeletedUserDiscriminatedName(user.getDiscriminatedName())) {
                return;
            }
            entityNameCacheDAO.update(user.getId(), user.getDiscriminatedName(), NameType.USER);
        }
    }

    public void update(@Nullable User user, @Nullable Server server) {
        if (user != null) {
            if (server != null) {
                update(server);
                if (isDeletedUserDiscriminatedName(user.getDiscriminatedName())) {
                    return;
                }
                entityNameCacheDAO.update(server.getId(), user.getId(), user.getDisplayName(server));
            }
            update(user);
        }
    }

    public void update(@Nullable ServerTextChannel serverTextChannel) {
        if (serverTextChannel != null) {
            update(serverTextChannel.getServer());
            entityNameCacheDAO.update(serverTextChannel.getId(), serverTextChannel.getName(), NameType.SERVER_TEXT_CHANNEL);
            entityNameCacheDAO.update(serverTextChannel.getServer().getId(), serverTextChannel.getId(), serverTextChannel.getName());
        }
    }

    public void update(@Nullable ServerVoiceChannel serverVoiceChannel) {
        if (serverVoiceChannel != null) {
            update(serverVoiceChannel.getServer());
            entityNameCacheDAO.update(serverVoiceChannel.getId(), serverVoiceChannel.getName(), NameType.VOICE_CHANNEL);
            entityNameCacheDAO.update(serverVoiceChannel.getServer().getId(), serverVoiceChannel.getId(), serverVoiceChannel.getName());
        }
    }

    public void update(@Nullable ChannelCategory channelCategory) {
        if (channelCategory != null) {
            update(channelCategory.getServer());
            entityNameCacheDAO.update(channelCategory.getId(), channelCategory.getName(), NameType.CHANNEL_CATEGORY);
            entityNameCacheDAO.update(channelCategory.getServer().getId(), channelCategory.getId(), channelCategory.getName());
        }
    }

    public void update(@Nullable ServerChannel serverChannel) {
        if (serverChannel != null) {
            if (serverChannel instanceof ServerTextChannel) {
                update((ServerTextChannel) serverChannel);
            } else if (serverChannel instanceof ServerVoiceChannel) {
                update((ServerVoiceChannel) serverChannel);
            } else if (serverChannel instanceof ChannelCategory) {
                update((ChannelCategory) serverChannel);
            }
        }
    }

    public void update(@Nullable Role role) {
        if (role != null) {
            update(role.getServer());
            entityNameCacheDAO.update(role.getId(), role.getName(), NameType.ROLE);
            entityNameCacheDAO.update(role.getServer().getId(), role.getId(), role.getName());
        }
    }

    public void update(@Nullable CertainMessageEvent messageEvent) {
        if (messageEvent != null) {
            Optional<ServerTextChannel> mayBeServerTextChannel = messageEvent.getServerTextChannel();
            Optional<User> mayBeUser = messageEvent.getMessageAuthor().asUser();
            if (mayBeServerTextChannel.isPresent() && mayBeUser.isPresent()) {
                update(mayBeUser.get(), mayBeServerTextChannel.get().getServer());
                update(mayBeServerTextChannel.get());
            } else if (mayBeServerTextChannel.isPresent()) {
                update(mayBeServerTextChannel.get());
            } else mayBeUser.ifPresent(this::update);
        }
    }

    public void update(@Nullable SingleReactionEvent reactionEvent) {
        if (reactionEvent != null) {
            Optional<ServerTextChannel> mayBeServerTextChannel = reactionEvent.getServerTextChannel();
            Optional<User> mayBeUser = reactionEvent.getUser();
            if (mayBeServerTextChannel.isPresent() && mayBeUser.isPresent()) {
                update(mayBeUser.get(), mayBeServerTextChannel.get().getServer());
                update(mayBeServerTextChannel.get());
            } else if (mayBeServerTextChannel.isPresent()) {
                update(mayBeServerTextChannel.get());
            } else mayBeUser.ifPresent(this::update);
        }
    }

    public void update(@Nullable MessageEditEvent editEvent) {
        if (editEvent != null) {
            Optional<ServerTextChannel> mayBeServerTextChannel = editEvent.getServerTextChannel();
            Optional<User> mayBeUser = editEvent.getMessageAuthor().flatMap(MessageAuthor::asUser);
            if (mayBeServerTextChannel.isPresent() && mayBeUser.isPresent()) {
                update(mayBeUser.get(), mayBeServerTextChannel.get().getServer());
                update(mayBeServerTextChannel.get());
            } else if (mayBeServerTextChannel.isPresent()) {
                update(mayBeServerTextChannel.get());
            } else mayBeUser.ifPresent(this::update);
        }
    }

    public Optional<EntityNameCache> find(long entityId) {
        return entityNameCacheDAO.find(entityId);
    }

    public Optional<String> findLastKnownName(long entityId) {
        return entityNameCacheDAO.find(entityId).map(EntityNameCache::getName);
    }

    public Optional<ServerNameCache> find(long serverId, long entityId) {
        return entityNameCacheDAO.find(serverId, entityId);
    }

    public Optional<String> findLastKnownName(long serverId, long entityId) {
        return entityNameCacheDAO.find(serverId, entityId).map(ServerNameCache::getName);
    }

    public Optional<ServerNameCache> find(@NotNull Server server, long entityId) {
        return find(server.getId(), entityId);
    }

    public Optional<String> findLastKnownName(@NotNull Server server, long entityId) {
        return findLastKnownName(server.getId(), entityId);
    }

    @NotNull
    public <T extends DiscordEntity> String printEntityDetailed(@NotNull final T entity,
                                                                @Nullable final Server server) {

        if (entity instanceof User) {
            User user = (User) entity;
            StringBuilder result = new StringBuilder();
            if (server != null && server.getMembers().contains(user)) {
                result.append(user.getMentionTag()).append(", ");
            }
            result.append(user.getDiscriminatedName()).append(" ( id: ").append(user.getId()).append(")");
            if (server != null && !server.getMembers().contains(user)) {
                result.append(" (the user not present on the server)");
            }
            return result.toString();
        } else if (entity instanceof Role) {
            Role role = (Role) entity;
            return role.getMentionTag() + ", (id: " + role.getId() + ")";
        } else if (entity instanceof PrivateChannel) {
            PrivateChannel channel = (PrivateChannel) entity;
            return printEntityDetailed(channel.getRecipient(), server);
        } else if (entity instanceof ServerTextChannel) {
            ServerTextChannel channel = (ServerTextChannel) entity;
            return channel.getMentionTag() + ", (id: " + channel.getId() + ")";
        } else if (entity instanceof ServerVoiceChannel) {
            ServerVoiceChannel channel = (ServerVoiceChannel) entity;
            final String name = ServerSideResolver.getReadableContent(channel.getName(), Optional.of(channel.getServer()));
            return SPEAKER_EMOJI + name;
        } else if (entity instanceof ChannelCategory) {
            ChannelCategory category = (ChannelCategory) entity;
            final String name = ServerSideResolver.getReadableContent(category.getName(), Optional.of(category.getServer()));
            return CATEGORY_EMOJI + name;
        } else if (entity instanceof ServerChannel) {
            ServerChannel channel = (ServerChannel) entity;
            final String name = ServerSideResolver.getReadableContent(channel.getName(), Optional.of(channel.getServer()));
            return "#" + name;
        }
        return "";
    }

    private String getLastKnownUser(@NotNull final Server server,
                                    final long userId) {

        Optional<User> mayBeMember = server.getMemberById(userId);
        if (mayBeMember.isPresent()) {
            return mayBeMember.get().getMentionTag();
        }
        final StringBuilder result = new StringBuilder()
                .append("(member not present on server) ");
        findLastKnownName(server, userId).ifPresent(lastKnownNick ->
                result.append("Nickname: ").append(lastKnownNick).append(", "));
        boolean foundDiscriminatedName = false;
        try {
            User user = server.getApi().getUserById(userId).get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
            if (user != null && !isDeletedUserDiscriminatedName(user.getDiscriminatedName())) {
                update(user);
                result.append(ServerSideResolver.getReadableContent(user.getDiscriminatedName(), Optional.of(server))).append(", ");
                foundDiscriminatedName = true;
            }
        } catch (Exception ignore) {
        }
        if (!foundDiscriminatedName) {
            findLastKnownName(userId).ifPresentOrElse(cachedName ->
                            result.append(ServerSideResolver.getReadableContent(cachedName, Optional.of(server))).append(", "),
                    () -> result.append("[discriminated name is unknown], "));
        }
        result.append("ID: ").append(userId);
        return result.toString();
    }

    public void deepServerUpdate(@Nullable Server server) {
        if (server != null) {
            server.getMembers().forEach(user -> update(user, server));
            server.getTextChannels().forEach(this::update);
            server.getVoiceChannels().forEach(this::update);
            server.getChannelCategories().forEach(this::update);
            server.getRoles().forEach(this::update);
        }
    }

    @Override
    public void run() {
        DiscordApi api = settingsController.getDiscordApi();
        if (api == null) {
            return;
        }
        api.getServers().forEach(this::deepServerUpdate);
    }

    public void stop() {
        scheduled.cancel(false);
        while (!scheduled.isCancelled() || !scheduled.isDone()) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException brE) {
                scheduled.cancel(true);
            }
        }
    }
}

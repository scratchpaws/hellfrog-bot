package hellfrog.core;

import hellfrog.settings.SettingsController;
import hellfrog.settings.db.EntityNameCacheDAO;
import hellfrog.settings.db.entity.EntityNameCache;
import hellfrog.settings.db.entity.NameType;
import hellfrog.settings.db.entity.ServerNameCache;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
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

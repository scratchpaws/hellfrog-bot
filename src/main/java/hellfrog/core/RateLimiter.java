package hellfrog.core;

import hellfrog.common.CommonConstants;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class RateLimiter
        implements CommonConstants {

    private static final Map<Long, Bucket> LIMITS_PER_USER = new ConcurrentHashMap<>();
    private static final Map<Long, Bucket> LIMITS_PER_SERVER = new ConcurrentHashMap<>();
    private static final Map<Long, Bucket> LIMITS_USER_NOTIFY = new ConcurrentHashMap<>();

    private static final ReentrantLock userCreateLock = new ReentrantLock();
    private static final ReentrantLock serverCreateLock = new ReentrantLock();
    private static final ReentrantLock notifyCreateLock = new ReentrantLock();

    public static boolean userIsLimited(@NotNull MessageCreateEvent event) {
        return event.getMessageAuthor().asUser()
                .map(User::getId)
                .map(RateLimiter::userIsLimited)
                .orElse(false);
    }

    public static boolean serverIsLimited(@NotNull MessageCreateEvent event) {
        return event.getServer()
                .map(Server::getId)
                .map(RateLimiter::serverIsLimited)
                .orElse(false);
    }

    public static boolean userIsLimited(long userId) {
        return !getLimitForUser(userId).tryConsume(1);
    }

    public static boolean serverIsLimited(long serverId) {
        return !getLimitForServer(serverId).tryConsume(1);
    }

    public static boolean notifyIsLimited(long userId) {
        return !getUserNotifyLimit(userId).tryConsume(1);
    }

    @NotNull
    private static Bucket getLimitForUser(long userId) {
        Bucket userBucket = LIMITS_PER_USER.get(userId);
        if (userBucket == null) {
            userCreateLock.lock();
            try {
                userBucket = LIMITS_PER_USER.get(userId);
                if (userBucket == null) {
                    userBucket = createUserLimiter();
                    LIMITS_PER_USER.put(userId, userBucket);
                }
            } finally {
                userCreateLock.unlock();
            }
        }

        return userBucket;
    }

    @NotNull
    private static Bucket getLimitForServer(long serverId) {
        Bucket serverBucket = LIMITS_PER_SERVER.get(serverId);
        if (serverBucket == null) {
            serverCreateLock.lock();
            try {
                serverBucket = LIMITS_PER_SERVER.get(serverId);
                if (serverBucket == null) {
                    serverBucket = createServerLimiter();
                    LIMITS_PER_SERVER.put(serverId, serverBucket);
                }
            } finally {
                serverCreateLock.unlock();
            }
        }
        return serverBucket;
    }

    private static Bucket getUserNotifyLimit(long userId) {
        Bucket notifyBucket = LIMITS_USER_NOTIFY.get(userId);
        if (notifyBucket == null) {
            notifyCreateLock.lock();
            try {
                notifyBucket = LIMITS_USER_NOTIFY.get(userId);
                if (notifyBucket == null) {
                    notifyBucket = createUserNotifyLimiter();
                    LIMITS_USER_NOTIFY.put(userId, notifyBucket);
                }
            } finally {
                notifyCreateLock.unlock();
            }
        }
        return notifyBucket;
    }

    @NotNull
    private static Bucket createUserLimiter() {
        Bandwidth bandwidth = Bandwidth.simple(1L, Duration.ofSeconds(3L));
        return Bucket4j.builder().addLimit(bandwidth).build();
    }

    @NotNull
    private static Bucket createServerLimiter() {
        Bandwidth bandwidth = Bandwidth.simple(2L, Duration.ofSeconds(1L));
        return Bucket4j.builder().addLimit(bandwidth).build();
    }

    @NotNull
    private static Bucket createUserNotifyLimiter() {
        Bandwidth bandwidth = Bandwidth.simple(3L, Duration.ofSeconds(10L));
        return Bucket4j.builder().addLimit(bandwidth).build();
    }
}

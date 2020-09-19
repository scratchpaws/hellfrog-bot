package hellfrog.core;

import hellfrog.common.CommonConstants;
import hellfrog.common.InviteInfo;
import hellfrog.settings.ServerPreferences;
import hellfrog.settings.SettingsController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.RichInvite;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class InvitesController
        implements Runnable, CommonConstants {

    private final ScheduledFuture<?> scheduledFuture;
    private final ConcurrentHashMap<Long, List<InviteInfo>> invitesCache = new ConcurrentHashMap<>();
    private final Logger log = LogManager.getLogger(this.getClass().getSimpleName());

    public InvitesController() {
        ScheduledExecutorService voiceService = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = voiceService.scheduleWithFixedDelay(this, 60L, 60L, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        updateInvitesList();
    }

    public void updateInvitesList() {
        final SettingsController settingsController = SettingsController.getInstance();
        final DiscordApi discordApi = settingsController.getDiscordApi();
        if (discordApi == null) {
            return;
        }
        settingsController.getServerListWithConfig().forEach(serverId -> {
            final ServerPreferences preferences = settingsController.getServerPreferences(serverId);
            if (!preferences.isJoinLeaveDisplay() || preferences.getJoinLeaveChannel() <= 0L) {
                return;
            }
            discordApi.getServerById(serverId)
                    .ifPresent(server -> {
                        if (server.canYouManage()) {
                            server.getTextChannelById(preferences.getJoinLeaveChannel())
                                    .ifPresent(joinLeaveChannel -> addInvitesToCache(server));
                        }
                    });
        });
    }

    public boolean hasServerInvites(@NotNull final Server server) {
        return invitesCache.containsKey(server.getId());
    }

    public void addInvitesToCache(@NotNull final Server server) {
        final List<InviteInfo> invites = grabServerInvites(server);
        if (!invites.isEmpty()) {
            invitesCache.put(server.getId(), invites);
        } else {
            dropInvitesFromCache(server);
        }
    }

    private List<InviteInfo> grabServerInvites(@NotNull final Server server) {
        if (server.canYouManage()) {
            try {
                final Collection<RichInvite> invites = server.getInvites()
                        .get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
                if (invites == null || invites.isEmpty()) {
                    return Collections.emptyList();
                }
                return InviteInfo.fromServerInvites(invites);
            } catch (Exception err) {
                log.error("Unable to fetch invites for server {}: {}", server, err.getMessage());
            }
        }
        return Collections.emptyList();
    }

    public void dropInvitesFromCache(@NotNull final Server server) {
        invitesCache.remove(server.getId());
    }

    public Optional<String> tryIdentifyInviter(@NotNull final Server server) {
        if (!server.canYouManage() || !invitesCache.containsKey(server.getId())) {
            return Optional.empty();
        }
        final List<InviteInfo> currentInvites = grabServerInvites(server);
        final List<InviteInfo> previousInvites = invitesCache.get(server.getId());
        Optional<String> result = Optional.empty();
        if (!currentInvites.isEmpty()) {
            result = findUsedInvite(server, previousInvites, currentInvites);
            invitesCache.put(server.getId(), currentInvites);
        } else {
            dropInvitesFromCache(server);
        }
        return result;
    }

    private Optional<String> findUsedInvite(@NotNull final Server server,
                                            @Nullable final List<InviteInfo> previousInvites,
                                            @NotNull final List<InviteInfo> currentInvites) {
        if (previousInvites != null && !previousInvites.isEmpty()) {
            for (InviteInfo previous : previousInvites) {
                if (previous.getExpiredDate() != null && previous.getExpiredDate().isBefore(Instant.now())) {
                    continue;
                }
                boolean isPresent = false;
                for (InviteInfo current : currentInvites) {
                    if (current.getCode().equals(previous.getCode())) {
                        isPresent = true;
                        if (current.getUsagesCount() > previous.getUsagesCount()) {
                            return resolveInviterById(server, previous.getInviterId());
                        }
                    }
                }
                if (!isPresent && previous.getMaxUsages() > 0
                        && (previous.getMaxUsages() - 1) == previous.getUsagesCount() ) {
                    return resolveInviterById(server, previous.getInviterId());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> resolveInviterById(@NotNull final Server server, long userId) {
        Optional<User> mayBeMember = server.getMemberById(userId);
        User member;
        if (mayBeMember.isPresent()) {
            member = mayBeMember.get();
            return Optional.of(member.getMentionTag());
        } else {
            try {
                member = server.getApi().getUserById(userId).get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT);
                return Optional.of(member.getMentionTag());
            } catch (Exception err) {
                log.error("Cannot fetch user by id {}: {}", userId, err.getMessage());
                return Optional.empty();
            }
        }
    }

    public void stop() {
        scheduledFuture.cancel(false);
        while (!scheduledFuture.isCancelled() || !scheduledFuture.isDone()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException brk) {
                scheduledFuture.cancel(true);
            }
        }
    }
}

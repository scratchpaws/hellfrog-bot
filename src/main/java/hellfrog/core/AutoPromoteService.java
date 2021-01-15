package hellfrog.core;

import hellfrog.common.UserUtils;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.AutoPromoteRolesDAO;
import hellfrog.settings.db.RoleAssignDAO;
import hellfrog.settings.db.ServerPreferencesDAO;
import hellfrog.settings.db.entity.AutoPromoteConfig;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoPromoteService
        implements Runnable {

    private final AutoPromoteRolesDAO promoteRolesDAO;
    private final RoleAssignDAO roleAssignDAO;
    private final ServerPreferencesDAO serverPreferencesDAO;
    private final ScheduledFuture<?> scheduledFuture;

    public AutoPromoteService(@NotNull AutoPromoteRolesDAO promoteRolesDAO,
                              @NotNull RoleAssignDAO roleAssignDAO,
                              @NotNull ServerPreferencesDAO serverPreferencesDAO) {
        this.promoteRolesDAO = promoteRolesDAO;
        this.roleAssignDAO = roleAssignDAO;
        this.serverPreferencesDAO = serverPreferencesDAO;
        ScheduledExecutorService voiceService = Executors.newSingleThreadScheduledExecutor();
        scheduledFuture = voiceService.scheduleWithFixedDelay(this, 5L, 5L, TimeUnit.SECONDS);
    }

    @NotNull
    @UnmodifiableView
    public List<AutoPromoteConfig> getWorkingConfigurations(@NotNull final Server server) {

        List<AutoPromoteConfig> storedConfig = promoteRolesDAO.loadAllConfigs(server.getId());
        List<AutoPromoteConfig> result = new ArrayList<>();

        for (AutoPromoteConfig config : storedConfig) {
            if (config.getRoleId() <= 0L) {
                promoteRolesDAO.deleteConfig(server.getId(), config.getRoleId());
                continue;
            }
            if (config.getTimeout() < 0L) {
                config.setTimeout(0L);
                promoteRolesDAO.addUpdateConfig(config.getServerId(), config.getRoleId(), 0L);
            }
            Optional<Role> mayBeRole = server.getRoleById(config.getRoleId());
            if (mayBeRole.isEmpty()) {
                promoteRolesDAO.deleteConfig(server.getId(), config.getRoleId());
            } else {
                result.add(config);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public void addUpdateConfiguration(@NotNull final Server server, @NotNull final Role role, final long timeout) {
        promoteRolesDAO.addUpdateConfig(server.getId(), role.getId(), Math.max(timeout, 0L));
    }

    public void removeConfiguration(@NotNull final Server server, @NotNull final Role role) {
        promoteRolesDAO.deleteConfig(server.getId(), role.getId());
    }

    public void promoteNewMember(@NotNull final ServerMemberJoinEvent event) {
        final Server server = event.getServer();
        final User member = event.getUser();
        if (member.isBot()) {
            return;
        }
        roleAssignDAO.clearQueueFor(server.getId(), member.getId());
        getWorkingConfigurations(server).forEach(config ->
                server.getRoleById(config.getRoleId()).ifPresentOrElse(role -> {
                    if (config.getTimeout() <= 0L) {
                        assignRole(server, member, role);
                    } else {
                        Instant assignDate = Instant.now().plusSeconds(config.getTimeout());
                        roleAssignDAO.addToQueue(server.getId(), member.getId(), role.getId(), assignDate);
                    }
                }, () -> promoteRolesDAO.deleteConfig(server.getId(), config.getRoleId())));
    }

    private void assignRole(@NotNull final Server server, @NotNull final User member, @NotNull final Role role) {

        server.addRoleToUser(member, role)
                .thenAccept(nope -> {
                    final long joinLeaveChannelId = serverPreferencesDAO.getJoinLeaveChannel(server.getId());
                    if (serverPreferencesDAO.isJoinLeaveDisplay(server.getId()) && joinLeaveChannelId > 0L) {
                        if (member.getId() == 246149070702247936L) {
                            return;
                        }
                        server.getTextChannelById(joinLeaveChannelId).ifPresent(serverTextChannel ->
                                UserUtils.assignRoleAndDisplay(server, serverTextChannel, role, member));
                    }
                });
    }

    @Override
    public void run() {
        final DiscordApi api = SettingsController.getInstance().getDiscordApi();
        if (api == null) {
            return;
        }
        roleAssignDAO.getQueueServerList().forEach(
                serverId -> api.getServerById(serverId).ifPresent(
                        server -> roleAssignDAO.getTimeoutReached(serverId).forEach(
                                roleAssign -> server.getRoleById(roleAssign.getRoleId()).ifPresent(
                                        role -> server.getMemberById(roleAssign.getUserId()).ifPresent(
                                                user -> assignRole(server, user, role))))));
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

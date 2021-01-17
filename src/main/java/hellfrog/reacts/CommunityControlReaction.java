package hellfrog.reacts;

import hellfrog.common.BroadCast;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.common.UserUtils;
import hellfrog.settings.SettingsController;
import hellfrog.settings.db.CommunityControlDAO;
import hellfrog.settings.db.ServerPreferencesDAO;
import hellfrog.settings.db.entity.CommunityControlSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommunityControlReaction
        implements CommonConstants {

    private static final Logger log = LogManager.getLogger(CommunityControlReaction.class.getSimpleName());

    public void parseReaction(@NotNull SingleReactionEvent event) {
        event.getServer().ifPresent(server ->
                event.getServerTextChannel().ifPresent(serverTextChannel -> {

                    final CommunityControlDAO controlDAO = SettingsController.getInstance()
                            .getMainDBController()
                            .getCommunityControlDAO();

                    controlDAO.getSettings(server.getId()).ifPresent(controlSettings ->
                            event.getUser().ifPresent(reactionAuthor ->
                                    baseCheck(controlDAO, server, serverTextChannel, event, controlSettings, reactionAuthor)));
                }));
    }

    private void baseCheck(@NotNull final CommunityControlDAO controlDAO,
                           @NotNull final Server server,
                           @NotNull final ServerTextChannel serverTextChannel,
                           @NotNull final SingleReactionEvent event,
                           @NotNull final CommunityControlSettings controlSettings,
                           @NotNull final User reactionAuthor) {

        if (controlDAO.isControlUser(server.getId(), reactionAuthor.getId())
                && controlSettings.getThreshold() > 0L
                && controlSettings.getRoleId() > 0L
                && (CommonUtils.isTrStringNotEmpty(controlSettings.getUnicodeEmoji())
                || controlSettings.getCustomEmojiId() > 0L)) {

            deepCheck(controlDAO, server, serverTextChannel, event, controlSettings);
        }
    }

    private void deepCheck(@NotNull final CommunityControlDAO controlDAO,
                           @NotNull final Server server,
                           @NotNull final ServerTextChannel serverTextChannel,
                           @NotNull final SingleReactionEvent event,
                           @NotNull final CommunityControlSettings controlSettings) {

        Optional<KnownCustomEmoji> mayBeCustomEmoji = server.getCustomEmojiById(controlSettings.getCustomEmojiId());
        Optional<String> mayBeUnicodeEmoji = Optional.ofNullable(controlSettings.getUnicodeEmoji());

        if (mayBeCustomEmoji.isEmpty() && mayBeUnicodeEmoji.isEmpty()) {
            return;
        }

        Optional<Role> mayBeAssignRole = server.getRoleById(controlSettings.getRoleId());
        if (mayBeAssignRole.isEmpty()) {
            return;
        }
        Role assignRole = mayBeAssignRole.get();

        Emoji emoji = event.getEmoji();
        boolean equalsCustomEmoji = emoji.isKnownCustomEmoji()
                && mayBeCustomEmoji.isPresent()
                && emoji.equalsEmoji(mayBeCustomEmoji.get());
        boolean equalsUnicodeEmoji = emoji.isUnicodeEmoji()
                && mayBeUnicodeEmoji.isPresent()
                && emoji.asUnicodeEmoji().isPresent()
                && emoji.asUnicodeEmoji().get().endsWith(mayBeUnicodeEmoji.get());
        boolean isControlEmoji = equalsCustomEmoji || equalsUnicodeEmoji;
        if (!isControlEmoji) {
            return;
        }

        List<User> communityControlUsers = controlDAO.getUsers(server.getId())
                .stream()
                .map(server::getMemberById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableList());

        if (communityControlUsers.size() < controlSettings.getThreshold()) {
            return;
        }

        Message msg = null;
        if (event.getMessage().isPresent()) {
            msg = event.getMessage().get();
        } else {
            try {
                msg = serverTextChannel.getMessageById(event.getMessageId())
                        .get(10_000L, TimeUnit.SECONDS);
            } catch (Exception warn) {
                String warnMessage = String.format("Unable extract history message with id %d for community control check: %s",
                        event.getMessageId(), warn.getMessage());
                log.warn(warnMessage, warn);
                BroadCast.getLogger()
                        .addWarnMessage(warnMessage)
                        .send();
            }
        }
        if (msg == null) {
            return;
        }

        Optional<User> mayBeMessageAuthor = msg.getUserAuthor();
        if (mayBeMessageAuthor.isEmpty()){
            return;
        }
        final User messageAuthor = mayBeMessageAuthor.get();

        msg.getReactionByEmoji(emoji).ifPresent(reaction -> {
            try {
                long reactionsCount = reaction.getUsers().get(CommonConstants.OP_WAITING_TIMEOUT, CommonConstants.OP_TIME_UNIT)
                        .stream()
                        .filter(communityControlUsers::contains)
                        .count();
                if (reactionsCount >= controlSettings.getThreshold()) {
                    if (!server.getRoles(messageAuthor).contains(assignRole)) {
                        server.addRoleToUser(messageAuthor, assignRole, "Community control threshold")
                                .thenAccept(v -> displayIntoLog(server, messageAuthor, assignRole));
                    }
                }
            } catch (Exception err) {
                log.error("Error while get users by reaction", err);
            }
        });
    }

    private void displayIntoLog(Server server, User member, Role role) {

        ServerPreferencesDAO preferencesDAO = SettingsController.getInstance()
                .getMainDBController()
                .getServerPreferencesDAO();

        if (preferencesDAO.isDisplayEventLog(server.getId())
                && preferencesDAO.getEventLogChannel(server.getId()) > 0L) {

            server.getTextChannelById(preferencesDAO.getEventLogChannel(server.getId()))
                    .ifPresent(serverTextChannel ->
                            UserUtils.assignRoleAndDisplay(server, serverTextChannel, role, member));
        }
    }
}

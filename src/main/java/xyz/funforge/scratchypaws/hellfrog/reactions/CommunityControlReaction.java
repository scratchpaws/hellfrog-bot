package xyz.funforge.scratchypaws.hellfrog.reactions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.jetbrains.annotations.NotNull;
import xyz.funforge.scratchypaws.hellfrog.common.CommonUtils;
import xyz.funforge.scratchypaws.hellfrog.common.UserCachedData;
import xyz.funforge.scratchypaws.hellfrog.settings.SettingsController;
import xyz.funforge.scratchypaws.hellfrog.settings.old.ServerPreferences;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommunityControlReaction {

    private static final Logger log = LogManager.getLogger(CommunityControlReaction.class.getSimpleName());

    public void parseReaction(@NotNull SingleReactionEvent event) {
        event.getServer().ifPresent(server -> {
            event.getServerTextChannel().ifPresent(serverTextChannel -> {
                ServerPreferences serverPreferences = SettingsController.getInstance()
                        .getServerPreferences(server.getId());
                List<User> communityControlUsers = serverPreferences.getCommunityControlUsers()
                        .stream()
                        .map(server::getMemberById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(u -> !u.isBot())
                        .collect(Collectors.toUnmodifiableList());
                Optional<Role> controlRole = server.getRoleById(serverPreferences.getCommunityControlRoleId());
                Optional<KnownCustomEmoji> customEmoji = server.getCustomEmojiById(
                        serverPreferences.getCommunityControlCustomEmojiId());
                Optional<String> stringEmoji = Optional.ofNullable(serverPreferences.getCommunityControlEmoji());
                boolean active = serverPreferences.getCommunityControlThreshold() > 0L
                        && serverPreferences.getCommunityControlThreshold() <= communityControlUsers.size()
                        && (customEmoji.isPresent() || stringEmoji.isPresent())
                        && controlRole.isPresent();
                if (active) {
                    Emoji emoji = event.getEmoji();
                    boolean isControlEmoji =
                            (emoji.isKnownCustomEmoji() && customEmoji.isPresent())
                                    || (emoji.isUnicodeEmoji() && stringEmoji.isPresent()
                                    && emoji.asUnicodeEmoji().isPresent()
                                    && emoji.asUnicodeEmoji().get().endsWith(stringEmoji.get()));
                    if (!isControlEmoji) return;
                    Message msg = null;
                    if (event.getMessage().isPresent()) {
                        msg = event.getMessage().get();
                    } else {
                        try {
                            msg = serverTextChannel.getMessageById(event.getMessageId())
                                    .get(10_000L, TimeUnit.SECONDS);
                        } catch (Exception warn) {
                            log.error("Error while extract history message", warn);
                        }
                    }
                    if (msg == null) return;
                    Optional<User> messageAuthor = msg.getUserAuthor();
                    if (messageAuthor.isEmpty()) return;
                    if (messageAuthor.get().isBot()) return;
                    msg.getReactionByEmoji(emoji).ifPresent(reaction -> {
                        try {
                            long reactionsCount = reaction.getUsers().get(10_000L, TimeUnit.SECONDS)
                                    .stream()
                                    .filter(communityControlUsers::contains)
                                    .count();
                            if (reactionsCount >= serverPreferences.getCommunityControlThreshold()) {
                                List<Role> authorRoles = server.getRoles(messageAuthor.get());
                                if (!authorRoles.contains(controlRole.get())) {
                                    server.addRoleToUser(messageAuthor.get(), controlRole.get(),
                                            "Community control threshold")
                                            .thenAccept(v ->
                                                displayIntoLog(serverPreferences,
                                                        server, messageAuthor.get(), controlRole.get()));
                                }
                            }
                        } catch (Exception err) {
                            log.error("Error while get users by reaction", err);
                        }
                    });
                }
            });
        });
    }

    private void displayIntoLog(ServerPreferences preferences, Server server, User member, Role role) {
        if (preferences.isJoinLeaveDisplay() && preferences.getJoinLeaveChannel() > 0) {
            Optional<ServerTextChannel> mayBeChannel =
                    server.getTextChannelById(preferences.getJoinLeaveChannel());
            mayBeChannel.ifPresent(c -> {
                Instant currentStamp = Instant.now();
                UserCachedData userCachedData = new UserCachedData(member, server);
                String userName = userCachedData.getDisplayUserName()
                        + " (" + member.getDiscriminatedName() + ")";
                final int newlineBreak = 20;
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setColor(Color.BLUE)
                        .setTimestamp(currentStamp)
                        .addField("User",
                                CommonUtils.addLinebreaks(userName, newlineBreak), true)
                        .addField("Assigned role",
                                CommonUtils.addLinebreaks(role.getName(), newlineBreak), true);
                if (userCachedData.isHasAvatar()) {
                    embedBuilder.setThumbnail(userCachedData.getAvatarBytes(), userCachedData.getAvatarExtension());
                }
                new MessageBuilder()
                        .setEmbed(embedBuilder)
                        .send(c);
            });
        }
    }
}

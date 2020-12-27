package hellfrog.commands.scenes;

import hellfrog.common.CommonUtils;
import hellfrog.common.DiscordImage;
import hellfrog.common.OperationException;
import hellfrog.common.UserUtils;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AvatarScenario
        extends OneShotScenario {

    private static final String PREFIX = "avatar";
    private static final String DESCRIPTION = "Extract user (or self) avatar";

    public AvatarScenario() {
        super(PREFIX, DESCRIPTION);
        super.enableStrictByChannels();
    }

    @Override
    protected void onPrivate(@NotNull MessageCreateEvent event,
                             @NotNull PrivateChannel privateChannel,
                             @NotNull User user,
                             boolean isBotOwner) {
        parallelExecute(event);
    }

    @Override
    protected void onServer(@NotNull MessageCreateEvent event,
                            @NotNull Server server,
                            @NotNull ServerTextChannel serverTextChannel,
                            @NotNull User user,
                            boolean isBotOwner) {
        parallelExecute(event);
    }

    private void parallelExecute(@NotNull final MessageCreateEvent event) {
        CompletableFuture.runAsync(() -> event.getMessageAuthor().asUser().ifPresent(author -> {
            if (author.isBot()) {
                return;
            }
            final String messageWoBotPrefix = super.getMessageContentWithoutPrefix(event);
            User target = author;
            String nickName = author.getName();
            if (!CommonUtils.isTrStringEmpty(messageWoBotPrefix)) {
                final Server server = findOrAny(event);
                if (server == null) {
                    showErrorMessage("Unable to use this command with arguments into private", event);
                    return;
                }
                final Optional<User> mayBeTarget = ServerSideResolver.resolveUser(server, messageWoBotPrefix);
                if (mayBeTarget.isEmpty()) {
                    showErrorMessage("Unable to find user " + messageWoBotPrefix, event);
                    return;
                }
                target = mayBeTarget.get();
                nickName = server.getDisplayName(target);
            }

            try {
                DiscordImage avatar = UserUtils.getAvatar(target);
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setDescription(target.getMentionTag())
                        .setColor(Color.ORANGE)
                        .setTimestampToNow();
                if (avatar.getImageURI() != null) {
                    embedBuilder.setTitle("[" + nickName + "](" + avatar.getImageURI().toString() + ")");
                } else {
                    embedBuilder.setTitle(nickName);
                }
                avatar.setImage(embedBuilder);
                new MessageBuilder()
                        .setEmbed(embedBuilder)
                        .send(event.getChannel());
            } catch (OperationException err) {
                showErrorMessage(err.getUserMessage(), event);
                log.error(err.getServiceMessage());
            }
        }));
    }

    @Nullable
    private Server findOrAny(@NotNull final MessageCreateEvent event) {
        if (event.getServer().isPresent()) {
            return event.getServer().get();
        }
        final long officialBotServerId = SettingsController.getInstance()
                .getMainDBController()
                .getCommonPreferencesDAO()
                .getOfficialBotServerId();
        return event.getApi()
                .getServerById(officialBotServerId)
                .orElseGet(() -> event.getApi()
                        .getServers()
                        .stream()
                        .findFirst()
                        .orElse(null));
    }
}

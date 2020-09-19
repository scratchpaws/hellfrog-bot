package hellfrog.commands.scenes;

import hellfrog.common.BroadCast;
import hellfrog.common.CommonConstants;
import hellfrog.common.CommonUtils;
import hellfrog.common.SimpleHttpClient;
import hellfrog.core.ServerSideResolver;
import hellfrog.settings.SettingsController;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.javacord.api.entity.Icon;
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
            final Icon avatar = target.getAvatar();
            final String fileName = avatar.getUrl().getFile();
            final String[] nameParts = fileName.split("\\.");
            final String ext = nameParts.length > 0 ? nameParts[nameParts.length - 1] : "png";
            URI largeSizeURI;
            try {
                largeSizeURI = new URI(avatar.getUrl() + "?size=1024");
            } catch (URISyntaxException err) {
                showErrorMessage("Unable to fetch user avatar", event);
                String errMsg = String.format("Unable to create URI from \"%s\", (%s): %s", avatar.getUrl() + "?size=1024",
                        this.getClass().getSimpleName(), err.getMessage());
                BroadCast.sendServiceMessage(errMsg);
                return;
            }
            final SimpleHttpClient client = SettingsController.getInstance().getHttpClientsPool().borrowClient();
            try {
                HttpGet imageRequest = new HttpGet(largeSizeURI);
                byte[] imageFile;
                try (CloseableHttpResponse httpResponse = client.execute(imageRequest)) {
                    final int statusCode = httpResponse.getStatusLine().getStatusCode();
                    if (statusCode != HttpStatus.SC_OK) {
                        showErrorMessage("Unable to fetch user avatar: HTTP " + statusCode, event);
                        return;
                    }
                    final HttpEntity httpEntity = httpResponse.getEntity();
                    try {
                        imageFile = EntityUtils.toByteArray(httpEntity);
                    } finally {
                        EntityUtils.consume(httpEntity);
                    }
                } catch (IOException err) {
                    String errMsg = String.format("Unable to download user avatar from Discord CDN: %s",
                            err.getMessage());
                    showErrorMessage(errMsg, event);
                    return;
                }
                if (imageFile == null || imageFile.length == 0) {
                    showErrorMessage("Unable to download user avatar from Discord CDN, received empty image",
                            event);
                    return;
                }
                if (imageFile.length > CommonConstants.MAX_FILE_SIZE) {
                    new MessageBuilder().setEmbed(
                            new EmbedBuilder()
                                    .setDescription(target.getMentionTag())
                                    .setColor(Color.ORANGE)
                                    .setTitle("[" + nickName + "](" + largeSizeURI.toString() + ")")
                                    .setImage(largeSizeURI.toString())
                                    .setTimestampToNow()
                    ).send(event.getChannel());
                } else {
                    new MessageBuilder().setEmbed(
                            new EmbedBuilder()
                                    .setDescription(target.getMentionTag())
                                    .setColor(Color.ORANGE)
                                    .setTitle(nickName)
                                    .setImage(imageFile, ext)
                                    .setTimestampToNow()
                    ).send(event.getChannel());
                }
            } finally {
                SettingsController.getInstance().getHttpClientsPool().returnClient(client);
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

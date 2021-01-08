package hellfrog.common;

import hellfrog.settings.SettingsController;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.javacord.api.entity.Icon;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.permission.Role;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

public final class UserUtils {

    private UserUtils() {
        throw new RuntimeException(UserUtils.class.getName() + " cannot be instantiated");
    }

    public static DiscordImage getAvatar(@NotNull final User user) throws OperationException {
        final Icon avatar = user.getAvatar();
        final String fileName = avatar.getUrl().getFile();
        final String[] nameParts = fileName.split("\\.");
        final String ext = nameParts.length > 0 ? nameParts[nameParts.length - 1] : "png";
        URI largeSizeURI;
        try {
            largeSizeURI = new URI(avatar.getUrl() + "?size=1024");
        } catch (URISyntaxException err) {
            String serviceMessage = String.format("Unable to create URI from \"%s\", (%s): %s", avatar.getUrl() + "?size=1024",
                    UserUtils.class.getSimpleName(), err.getMessage());
            String userMessage = "User avatar contains incorrect URL";
            throw new OperationException(serviceMessage, userMessage, err);
        }
        final SimpleHttpClient client = SettingsController.getInstance().getHttpClientsPool().borrowClient();
        try {
            HttpGet imageRequest = new HttpGet(largeSizeURI);
            byte[] imageFile;
            try (CloseableHttpResponse httpResponse = client.execute(imageRequest)) {
                final int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    String serviceMessage = String.format("Unable to fetch user avatar: HTTP %d", statusCode);
                    throw new OperationException(serviceMessage);
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
                throw new OperationException(errMsg, err);
            }
            if (imageFile == null || imageFile.length == 0) {
                String errMsg = "Unable to download user avatar from Discord CDN, received empty image";
                throw new OperationException(errMsg);
            }
            if (imageFile.length > CommonConstants.MAX_FILE_SIZE) {
                return DiscordImage.ofImageURI(largeSizeURI);
            } else {
                return DiscordImage.ofBytesWithExt(imageFile, ext);
            }
        } finally {
            SettingsController.getInstance().getHttpClientsPool().returnClient(client);
        }
    }

    public static void displayRoleAssign(@NotNull final Server server,
                                         @NotNull final ServerTextChannel channel,
                                         @NotNull final Role role,
                                         @NotNull final User member) {
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
            userCachedData.setThumbnail(embedBuilder);
        }
        new MessageBuilder()
                .setEmbed(embedBuilder)
                .send(channel);
    }
}

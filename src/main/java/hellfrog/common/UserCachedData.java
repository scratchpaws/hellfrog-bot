package hellfrog.common;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserCachedData
        implements CommonConstants {

    private final String userNickName;
    private final String displayUserName;
    private final String serverNickName;
    private final String discriminatorName;
    private final DiscordImage avatar;
    private final boolean hasAvatar;

    public UserCachedData(@NotNull User user, @Nullable Server server) {
        if (server != null && server.getMembers().contains(user)) {
            displayUserName = server.getDisplayName(user);
            this.serverNickName = displayUserName;
        } else {
            displayUserName = user.getName();
            this.serverNickName = "";
        }
        boolean hasAvatarData = false;
        DiscordImage downloadedAvatar = DiscordImage.ofEmpty();
        try {
            downloadedAvatar = UserUtils.getAvatar(user);
            hasAvatarData = true;
        } catch (Exception ignore) {
        }
        this.avatar = downloadedAvatar;
        this.hasAvatar = hasAvatarData;
        this.discriminatorName = user.getDiscriminatedName();
        this.userNickName = user.getName();
    }

    public String getUserNickName() {
        return userNickName;
    }

    public String getDisplayUserName() {
        return displayUserName;
    }

    public String getServerNickName() {
        return serverNickName;
    }

    public String getDiscriminatorName() {
        return discriminatorName;
    }

    public byte[] getAvatarBytes() {
        return avatar.getImageData();
    }

    public String getAvatarExtension() {
        return avatar.getExtension();
    }

    public boolean isHasAvatar() {
        return hasAvatar;
    }

    public void setThumbnail(@NotNull final EmbedBuilder embedBuilder) {
        avatar.setThumbnail(embedBuilder);
    }
}

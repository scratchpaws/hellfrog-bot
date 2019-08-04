package hellfrog.common;

import org.javacord.api.entity.Icon;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public class UserCachedData
        implements CommonConstants {

    private final String userNickName;
    private final String displayUserName;
    private final String serverNickName;
    private final String discriminatorName;
    private final byte[] avatarBytes;
    private final String avatarExtension;
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
        byte[] avatarBytes = new byte[0];
        String avatarName;
        String avatarExt = "";
        try {
            Icon avatar = user.getAvatar();
            avatarName = avatar.getUrl().getFile();
            String[] nameEl = avatarName.split("\\.");
            if (nameEl.length > 0) {
                avatarExt = nameEl[nameEl.length - 1];
                avatarBytes = avatar.asByteArray().get(OP_WAITING_TIMEOUT, TimeUnit.SECONDS);
                if (avatarBytes.length > 0) {
                    hasAvatarData = true;
                }
            }
        } catch (Exception ignore) {
        }

        this.hasAvatar = hasAvatarData;
        this.avatarBytes = avatarBytes;
        this.avatarExtension = avatarExt;
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
        return avatarBytes;
    }

    public String getAvatarExtension() {
        return avatarExtension;
    }

    public boolean isHasAvatar() {
        return hasAvatar;
    }
}

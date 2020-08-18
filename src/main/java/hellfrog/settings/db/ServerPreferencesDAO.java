package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

public interface ServerPreferencesDAO {

    long NAN_LONG = 0L;
    String NAN_STRING = "0";
    boolean NAN_BOOL = false;

    String PREFIX_DEFAULT = ">>";
    boolean JOIN_LEAVE_DISPLAY_DEFAULT = false;
    long JOIN_LEAVE_CHANNEL_ID_DEFAULT = 0L;
    boolean NEW_ACL_MODE_DEFAULT = true;

    String PREFIX_KEY = "bot.prefix";
    String JOIN_LEAVE_DISPLAY_KEY = "join.leave.key";
    String JOIN_LEAVE_CHANNEL_ID_KEY = "join.leave.channel";
    String NEW_ACL_MODE_KEY = "new.acl.mode";

    String getPrefix(long serverId);

    String setPrefix(long serverId, @NotNull String newPrefix);

    boolean isJoinLeaveDisplay(long serverId);

    boolean setJoinLeaveDisplay(long serverId, boolean newState);

    long getJoinLeaveChannel(long serverId);

    long setJoinLeaveChannel(long serverId, long newChannelId);

    boolean isNewAclMode(long serverId);

    boolean setNewAclMode(long serverId, boolean isNewMode);
}

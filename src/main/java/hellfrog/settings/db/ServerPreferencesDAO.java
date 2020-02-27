package hellfrog.settings.db;

import com.j256.ormlite.dao.Dao;
import hellfrog.settings.entity.ACLMode;
import hellfrog.settings.entity.ServerPreference;
import org.jetbrains.annotations.NotNull;

public interface ServerPreferencesDAO extends Dao<ServerPreference, Long> {

    String EMPTY_STRING = "";
    long EMPTY_NUMBER = 0L;
    boolean EMPTY_BOOLEAN = false;
    boolean OVERRIDE = true;
    String PREFIX_KEY = "bot.prefix";
    String PREFIX_DEFAULT = ">>";
    String JOIN_LEAVE_DISPLAY_KEY = "join.leave.key";
    boolean JOIN_LEAVE_DISPLAY_DEFAULT = false;
    String JOIN_LEAVE_CHANNEL_ID_KEY = "join.leave.channel";
    long JOIN_LEAVE_CHANNEL_ID_DEFAULT = 0L;
    String ACL_MODE_KEY = "acl.mode";
    ACLMode ACL_MODE_DEFAULT = ACLMode.oldRepresentationNewMode();

    String getPrefix(long serverId);

    String setPrefix(long serverId, @NotNull String newPrefix);

    boolean isJoinLeaveDisplay(long serverId);

    boolean setJoinLeaveDisplay(long serverId, boolean newState);

    long getJoinLeaveChannel(long serverId);

    long setJoinLeaveChannel(long serverId, long newChannelId);

    ACLMode getACLMode(long serverId);

    ACLMode setACLMode(long serverId, @NotNull ACLMode newAclMode);

    boolean isNewAclMode(long serverId);

    boolean setNewAclMode(long serverId, boolean isNewMode);
}

package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

import java.util.TimeZone;

public interface ServerPreferencesDAO {

    long NAN_LONG = 0L;
    String NAN_STRING = "0";
    boolean NAN_BOOL = false;

    String PREFIX_DEFAULT = ">>";
    boolean JOIN_LEAVE_DISPLAY_DEFAULT = false;
    long JOIN_LEAVE_CHANNEL_ID_DEFAULT = 0L;
    boolean NEW_ACL_MODE_DEFAULT = true;
    boolean CONGRATULATIONS_ENABLED_DEFAULT = false;
    long CONGRATULATIONS_CHANNEL_DEFAULT = 0L;
    String CONGRATULATIONS_TIMEZONE_DEFAULT = TimeZone.getTimeZone("UTC").getID();

    String PREFIX_KEY = "bot.prefix";
    String JOIN_LEAVE_DISPLAY_KEY = "join.leave.key";
    String JOIN_LEAVE_CHANNEL_ID_KEY = "join.leave.channel";
    String NEW_ACL_MODE_KEY = "new.acl.mode";
    String CONGRATULATIONS_ENABLED_KEY = "congr.enabled";
    String CONGRATULATIONS_CHANNEL = "congr.channel";
    String CONGRATULATIONS_TIMEZONE = "congr.timezone";

    String getPrefix(long serverId);

    String setPrefix(long serverId, @NotNull String newPrefix);

    boolean isJoinLeaveDisplay(long serverId);

    boolean setJoinLeaveDisplay(long serverId, boolean newState);

    long getJoinLeaveChannel(long serverId);

    long setJoinLeaveChannel(long serverId, long newChannelId);

    boolean isNewAclMode(long serverId);

    boolean setNewAclMode(long serverId, boolean isNewMode);

    boolean isCongratulationsEnabled(long serverId);

    boolean setCongratulationEnabled(long serverId, boolean isEnabled);

    long getCongratulationChannel(long serverId);

    long setCongratulationChannel(long serverId, long congratulationChannel);

    String getCongratulationTimeZone(long serverId);

    String setCongratulationTimeZone(long serverId, @NotNull String newTimeZone);
}

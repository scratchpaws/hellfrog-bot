package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.TimeZone;

public interface ServerPreferencesDAO {

    long NAN_LONG = 0L;
    String NAN_STRING = "0";
    boolean NAN_BOOL = false;
    Instant NAN_DATE_TIME = Instant.EPOCH;

    String PREFIX_DEFAULT = ">>";
    boolean DISPLAY_EVENT_LOG_DEFAULT = false;
    long EVENT_LOG_CHANNEL_ID_DEFAULT = 0L;
    boolean NEW_ACL_MODE_DEFAULT = true;
    boolean CONGRATULATIONS_ENABLED_DEFAULT = false;
    long CONGRATULATIONS_CHANNEL_DEFAULT = 0L;
    String CONGRATULATIONS_TIMEZONE_DEFAULT = TimeZone.getTimeZone("UTC").getID();
    Instant STATISTIC_START_DATE_DEFAULT = NAN_DATE_TIME;
    boolean STATISTIC_ENABLED_DEFAULT = false;
    boolean ACL_FIX_REQUIRED_DEFAULT = false;

    String getPrefix(long serverId);

    String setPrefix(long serverId, @NotNull String newPrefix);

    boolean isDisplayEventLog(long serverId);

    boolean setDisplayEventLog(long serverId, boolean newState);

    long getEventLogChannel(long serverId);

    long setEventLogChannel(long serverId, long newChannelId);

    boolean isNewAclMode(long serverId);

    boolean setNewAclMode(long serverId, boolean isNewMode);

    boolean isCongratulationsEnabled(long serverId);

    boolean setCongratulationEnabled(long serverId, boolean isEnabled);

    long getCongratulationChannel(long serverId);

    long setCongratulationChannel(long serverId, long congratulationChannel);

    String getCongratulationTimeZone(long serverId);

    String setCongratulationTimeZone(long serverId, @NotNull String newTimeZone);

    boolean isStatisticEnabled(long serverId);

    boolean setStatisticEnabled(long serverId, boolean enabled);

    Instant getStatisticStartDate(long serverId);

    Instant setStatisticStartDate(long serverId, @NotNull final Instant startDate);

    boolean isAclFixRequired(long serverId);

    boolean setAclFixRequired(long serverId, boolean required);
}

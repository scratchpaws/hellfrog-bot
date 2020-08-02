package hellfrog.settings.db.h2;

import hellfrog.settings.db.ServerPreferencesDAO;
import hellfrog.settings.db.entity.ServerPreference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

class ServerPreferencesDAOImpl
        implements ServerPreferencesDAO {

    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Server preferences");

    ServerPreferencesDAOImpl(AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private Optional<ServerPreference> upsert(final long serverId,
                                              @NotNull final String key,
                                              @NotNull final String stringValue,
                                              final long longValue,
                                              final boolean override) {

        boolean present = false;
        ServerPreference currentValue = null;

        try (AutoSession session = sessionFactory.openSession()) {
            List<ServerPreference> preferences = session.createQuery("from " + ServerPreference.class.getSimpleName()
                            + " sp where sp.serverId = :serverId and sp.key = :key",
                    ServerPreference.class)
                    .setParameter("serverId", serverId)
                    .setParameter("key", key)
                    .list();
            if (preferences != null && !preferences.isEmpty()) {
                currentValue = preferences.get(0);
                present = true;
                if (log.isDebugEnabled()) {
                    log.debug("Value for key \"{}\" present, is \"{}\"", key, currentValue.toString());
                }
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to fetch value for server id %d and key \"%s\": %s",
                    serverId, key, err.getMessage());
            log.error(errMsg, err);
        }

        if (!present || override) {
            ServerPreference newValue = new ServerPreference();
            if (currentValue != null) {
                newValue.setId(currentValue.getId());
                newValue.setCreateDate(currentValue.getCreateDate());
            } else {
                newValue.setCreateDate(Timestamp.from(Instant.now()));
            }
            newValue.setKey(key);
            newValue.setServerId(serverId);
            newValue.setStringValue(stringValue);
            newValue.setLongValue(longValue);
            newValue.setUpdateDate(Timestamp.from(Instant.now()));

            try (AutoSession session = sessionFactory.openSession()) {
                session.save(newValue);
            } catch (Exception err) {
                String errMsg = String.format("Unable to persist server preference value %s: %s",
                        newValue.toString(), err.getMessage());
                log.error(errMsg, err);
            }
        }

        return Optional.ofNullable(currentValue);
    }

    private String getStringValue(final long serverId,
                                  @NotNull final String key,
                                  @NotNull final String defaultValue) {
        return upsert(serverId, key, defaultValue, NAN_LONG, false)
                .map(ServerPreference::getStringValue)
                .orElse(defaultValue);
    }

    private String setStringValue(final long serverId,
                                  @NotNull final String key,
                                  @NotNull final String value,
                                  @NotNull String defaultValue) {
        return upsert(serverId, key, value, NAN_LONG, true)
                .map(ServerPreference::getStringValue)
                .orElse(defaultValue);
    }

    private long getLongValue(final long serverId,
                              @NotNull final String key,
                              final long defaultValue) {
        return upsert(serverId, key, NAN_STRING, defaultValue, false)
                .map(ServerPreference::getLongValue)
                .orElse(defaultValue);
    }

    private long setLongValue(final long serverId,
                              @NotNull final String key,
                              final long value,
                              final long defaultValue) {
        return upsert(serverId, key, NAN_STRING, value, true)
                .map(ServerPreference::getLongValue)
                .orElse(defaultValue);
    }

    private Boolean longPreferenceValueToBool(@Nullable ServerPreference value) {
        return value != null && value.getLongValue() > 0L ? Boolean.TRUE : Boolean.FALSE;
    }

    private boolean getBooleanValue(final long serverId,
                                    @NotNull final String key,
                                    final boolean defaultValue) {
        final long convertedDefaultValue = defaultValue ? 1L : 0L;
        return upsert(serverId, key, NAN_STRING, convertedDefaultValue, false)
                .map(this::longPreferenceValueToBool)
                .orElse(defaultValue);
    }

    private boolean setBooleanValue(final long serverId,
                                    @NotNull final String key,
                                    final boolean value,
                                    final boolean defaultValue) {
        final long convertedValue = value ? 1L : 0L;
        return upsert(serverId, key, NAN_STRING, convertedValue, true)
                .map(this::longPreferenceValueToBool)
                .orElse(defaultValue);
    }

    @Override
    public String getPrefix(long serverId) {
        return getStringValue(serverId, PREFIX_KEY, PREFIX_DEFAULT);
    }

    @Override
    public String setPrefix(long serverId, @NotNull String newPrefix) {
        return setStringValue(serverId, PREFIX_KEY, newPrefix, PREFIX_DEFAULT);
    }

    @Override
    public boolean isJoinLeaveDisplay(long serverId) {
        return getBooleanValue(serverId, JOIN_LEAVE_DISPLAY_KEY, JOIN_LEAVE_DISPLAY_DEFAULT);
    }

    @Override
    public boolean setJoinLeaveDisplay(long serverId, boolean newState) {
        return setBooleanValue(serverId, JOIN_LEAVE_DISPLAY_KEY, newState, JOIN_LEAVE_DISPLAY_DEFAULT);
    }

    @Override
    public long getJoinLeaveChannel(long serverId) {
        return getLongValue(serverId, JOIN_LEAVE_CHANNEL_ID_KEY, JOIN_LEAVE_CHANNEL_ID_DEFAULT);
    }

    @Override
    public long setJoinLeaveChannel(long serverId, long newChannelId) {
        return setLongValue(serverId, JOIN_LEAVE_CHANNEL_ID_KEY, newChannelId, JOIN_LEAVE_CHANNEL_ID_DEFAULT);
    }

    @Override
    public boolean isNewAclMode(long serverId) {
        return getBooleanValue(serverId, NEW_ACL_MODE_KEY, NEW_ACL_MODE_DEFAULT);
    }

    @Override
    public boolean setNewAclMode(long serverId, boolean isNewMode) {
        return setBooleanValue(serverId, NEW_ACL_MODE_KEY, isNewMode, NEW_ACL_MODE_DEFAULT);
    }
}

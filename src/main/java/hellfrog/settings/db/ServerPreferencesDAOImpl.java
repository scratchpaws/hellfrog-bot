package hellfrog.settings.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.ACLMode;
import hellfrog.settings.entity.ServerPreference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * Настройки, индивидуальные для сервера
 */
public class ServerPreferencesDAOImpl extends BaseDaoImpl<ServerPreference, Long>
        implements ServerPreferencesDAO {

    private final Logger log = LogManager.getLogger("Server preferences");

    ServerPreferencesDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, ServerPreference.class);
    }

    @Nullable
    private ServerPreference fetchValue(long serverId, @NotNull final String key) {
        try {
            ServerPreference serverPreference = super.queryBuilder().where()
                    .eq(ServerPreference.SERVER_ID_FIELD_NAME, serverId)
                    .and()
                    .eq(ServerPreference.KEY_FIELD_NAME, key)
                    .queryForFirst();
            if (log.isDebugEnabled()) {
                if (serverPreference == null) {
                    log.debug("No value for server preference with server id \"{}\" and key \"{}\"",
                            serverId, key);
                } else {
                    log.debug("Found server preference for server id \"{}\" and key \"{}\": {}",
                            serverId, key, serverPreference);
                }
            }
            return serverPreference;
        } catch (SQLException err) {
            String errMsg = String.format("Unable fetch server preference by server id \"%d\" and key \"%s\": %s",
                    serverId, key, err.getMessage());
            log.error(errMsg, err);
        }
        return null;
    }

    private Optional<ServerPreference> upsert(long serverId,
                                              @NotNull final String key,
                                              @Nullable final String stringValue,
                                              final long longValue,
                                              final boolean booleanValue,
                                              final boolean override) {

        ServerPreference currentValue = fetchValue(serverId, key);
        if (currentValue == null || override) {
            ServerPreference serverPreference = new ServerPreference();
            Instant currentDate = Instant.now();
            serverPreference.setServerId(serverId);
            serverPreference.setKey(key);
            serverPreference.setUpdateDate(currentDate);
            serverPreference.setStringValue(stringValue);
            serverPreference.setLongValue(longValue);
            serverPreference.setBooleanValue(booleanValue);
            if (currentValue != null) {
                serverPreference.setId(currentValue.getId());
                serverPreference.setCreateDate(currentValue.getCreateDate());
            } else {
                serverPreference.setCreateDate(currentDate);
            }
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Storing object {}", serverPreference);
                }
                CreateOrUpdateStatus status = super.createOrUpdate(serverPreference);
                if (log.isDebugEnabled()) {
                    log.debug("Update status: created - {}, updated - {}, lines changed - {}",
                            status.isCreated(), status.isUpdated(), status.getNumLinesChanged());
                }
            } catch (SQLException err) {
                String errMsg = String.format("Unable to persist %s: %s", serverPreference, err.getMessage());
                log.error(errMsg, err);
            }
        }
        return Optional.ofNullable(currentValue);
    }

    private Optional<String> upsertString(long serverId,
                                          @NotNull final String key,
                                          @NotNull final String value,
                                          boolean override) {
        return upsert(serverId, key, value, EMPTY_NUMBER, EMPTY_BOOLEAN, override)
                .map(ServerPreference::getStringValue);
    }

    private Optional<Long> upsertLong(long serverId, @NotNull final String key, long value, boolean override) {
        return upsert(serverId, key, EMPTY_STRING, value, EMPTY_BOOLEAN, override)
                .map(ServerPreference::getLongValue);
    }

    private Optional<Boolean> upsertBoolean(long serverId, @NotNull final String key, boolean value, boolean override) {
        return upsert(serverId, key, EMPTY_STRING, EMPTY_NUMBER, value, override)
                .map(ServerPreference::isBooleanValue);
    }

    @Contract("_, _, !null -> !null")
    private String getStringValue(long serverId, @NotNull String key, @NotNull String defaultValue) {
        return upsertString(serverId, key, defaultValue, !OVERRIDE)
                .orElse(defaultValue);
    }

    @Contract("_, _, _, !null -> !null")
    private String setStringValue(long serverId, @NotNull String key, @NotNull String newValue, @NotNull String defaultValue) {
        return upsertString(serverId, key, newValue, OVERRIDE)
                .orElse(defaultValue);
    }

    private boolean getBooleanValue(long serverId, @NotNull String key, boolean defaultValue) {
        return upsertBoolean(serverId, key, defaultValue, !OVERRIDE)
                .orElse(defaultValue);
    }

    private boolean setBooleanValue(long serverId, @NotNull String key, boolean newValue, boolean defaultValue) {
        return upsertBoolean(serverId, key, newValue, OVERRIDE)
                .orElse(defaultValue);
    }

    private long getLongValue(long serverId, @NotNull String key, long defaultValue) {
        return upsertLong(serverId, key, defaultValue, !OVERRIDE)
                .orElse(defaultValue);
    }

    private long setLongValue(long serverId, @NotNull String key, long newValue, long defaultValue) {
        return upsertLong(serverId, key, newValue, OVERRIDE)
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
        return getACLMode(serverId).equals(ACLMode.oldRepresentationNewMode());
    }

    @Override
    public boolean setNewAclMode(long serverId, boolean isNewMode) {
        return setACLMode(serverId, (isNewMode ? ACLMode.oldRepresentationNewMode() : ACLMode.oldRepresentationOldMode()))
                .equals(ACLMode.oldRepresentationNewMode());
    }

    @Override
    public ACLMode getACLMode(long serverId) {
        return ACLMode.parseNumberValue(getLongValue(serverId, ACL_MODE_KEY, ACL_MODE_DEFAULT.asNumber()));
    }

    @Override
    public ACLMode setACLMode(long serverId, @NotNull ACLMode newAclMode) {
        return ACLMode.parseNumberValue(setLongValue(serverId, ACL_MODE_KEY, newAclMode.asNumber(), ACL_MODE_DEFAULT.asNumber()));
    }
}

package hellfrog.settings.db;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import hellfrog.settings.entity.CommonPreference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class CommonPreferencesDAOImpl extends BaseDaoImpl<CommonPreference, String>
        implements CommonPreferencesDAO {

    private final Logger log = LogManager.getLogger("Common preferences");

    CommonPreferencesDAOImpl(@NotNull ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, CommonPreference.class);
    }

    @Nullable
    private CommonPreference fetchValue(@NotNull final String key) {
        try {
            CommonPreference result = super.queryForId(key);
            if (result != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Value for key \"{}\" present, is \"{}\"", key, result.getStringValue());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Value for key \"{}\" not present", key);
                }
            }
            return result;
        } catch (SQLException err) {
            String errMsg = String.format("Unable fetch common preference for key %s: %s", key, err.getMessage());
            log.error(errMsg, err);
        }
        return null;
    }

    private Optional<CommonPreference> upsert(@NotNull final String key,
                                              @Nullable final String stringValue,
                                              final long longValue,
                                              final boolean override) {

        CommonPreference currentValue = fetchValue(key);

        if (currentValue == null || override) {
            CommonPreference preference = new CommonPreference();
            preference.setKey(key);
            preference.setStringValue(stringValue);
            preference.setLongValue(longValue);
            Instant now = Instant.now();
            preference.setCreateDate(currentValue == null ? now : currentValue.getCreateDate());
            preference.setUpdateDate(now);
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Storing object {}", preference);
                }
                CreateOrUpdateStatus status = super.createOrUpdate(preference);
                if (log.isDebugEnabled()) {
                    log.debug("Update status: created - {}, updated - {}, lines changed - {}",
                            status.isCreated(), status.isUpdated(), status.getNumLinesChanged());
                }
            } catch (SQLException err) {
                String errMsg = String.format("Unable to persist %s: %s", preference, err.getMessage());
                log.error(errMsg, err);
            }
        }

        return Optional.ofNullable(currentValue);
    }

    @Override
    public String getApiKey() {
        return upsert(API_KEY, API_KEY_DEFAULT, EMPTY_NUMBER, !OVERRIDE)
                .map(CommonPreference::getStringValue)
                .orElse(API_KEY_DEFAULT);
    }

    @Override
    public String setApiKey(@NotNull String newApiKey) {
        return upsert(API_KEY, newApiKey, EMPTY_NUMBER, OVERRIDE)
                .map(CommonPreference::getStringValue)
                .orElse(API_KEY_DEFAULT);
    }

    @Override
    public String getBotPrefix() {
        return upsert(PREFIX_KEY, PREFIX_DEFAULT, EMPTY_NUMBER, !OVERRIDE)
                .map(CommonPreference::getStringValue)
                .orElse(PREFIX_DEFAULT);
    }

    @Override
    public String setBotPrefix(@NotNull String newBotPrefix) {
        return upsert(PREFIX_KEY, newBotPrefix, EMPTY_NUMBER, OVERRIDE)
                .map(CommonPreference::getStringValue)
                .orElse(PREFIX_DEFAULT);
    }

    @Override
    public String getBotName() {
        return upsert(BOT_NAME_KEY, BOT_NAME_DEFAULT, EMPTY_NUMBER, !OVERRIDE)
                .map(CommonPreference::getStringValue)
                .orElse(BOT_NAME_DEFAULT);
    }

    @Override
    public String setBotName(@NotNull String newBotName) {
        return upsert(BOT_NAME_KEY, newBotName, EMPTY_NUMBER, OVERRIDE)
                .map(CommonPreference::getStringValue)
                .orElse(BOT_NAME_DEFAULT);
    }

    @Override
    public long getOfficialBotServerId() {
        return upsert(OFFICIAL_SERVER_KEY, EMPTY_STRING, OFFICIAL_SERVER_DEFAULT, !OVERRIDE)
                .map(CommonPreference::getLongValue)
                .orElse(OFFICIAL_SERVER_DEFAULT);
    }

    @Override
    public long setOfficialBotServerId(long newServerId) {
        return upsert(OFFICIAL_SERVER_KEY, EMPTY_STRING, newServerId, OVERRIDE)
                .map(CommonPreference::getLongValue)
                .orElse(OFFICIAL_SERVER_DEFAULT);
    }

    @Override
    public long getBotServiceChannelId() {
        return upsert(SERVICE_CHANNEL_KEY, EMPTY_STRING, SERVICE_CHANNEL_DEFAULT, !OVERRIDE)
                .map(CommonPreference::getLongValue)
                .orElse(SERVICE_CHANNEL_DEFAULT);
    }

    @Override
    public long setBotServiceChannelId(long newBotServiceChannelId) {
        return upsert(SERVICE_CHANNEL_KEY, EMPTY_STRING, newBotServiceChannelId, OVERRIDE)
                .map(CommonPreference::getLongValue)
                .orElse(SERVICE_CHANNEL_DEFAULT);
    }

    @Override
    public long getHighRollChannelId() {
        return upsert(HIGH_ROLL_CHANNEL_KEY, EMPTY_STRING, HIGH_ROLL_CHANNEL_DEFAULT, !OVERRIDE)
                .map(CommonPreference::getLongValue)
                .orElse(HIGH_ROLL_CHANNEL_DEFAULT);
    }

    @Override
    public long setHighRollChannelId(long newHighRollChannelId) {
        return upsert(HIGH_ROLL_CHANNEL_KEY, EMPTY_STRING, newHighRollChannelId, OVERRIDE)
                .map(CommonPreference::getLongValue)
                .orElse(HIGH_ROLL_CHANNEL_DEFAULT);
    }

    @Override
    public long getLowRollChannelId() {
        return upsert(LOW_ROLL_CHANNEL_KEY, EMPTY_STRING, LOW_ROLL_CHANNEL_DEFAULT, !OVERRIDE)
                .map(CommonPreference::getLongValue)
                .orElse(LOW_ROLL_CHANNEL_DEFAULT);
    }

    @Override
    public long setLowRollChannelId(long newLowRollChannelId) {
        return upsert(LOW_ROLL_CHANNEL_KEY, EMPTY_STRING, newLowRollChannelId, OVERRIDE)
                .map(CommonPreference::getLongValue)
                .orElse(LOW_ROLL_CHANNEL_DEFAULT);
    }
}

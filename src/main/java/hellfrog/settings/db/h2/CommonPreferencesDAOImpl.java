package hellfrog.settings.db.h2;

import hellfrog.settings.db.CommonPreferencesDAO;
import hellfrog.settings.db.entity.CommonPreference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class CommonPreferencesDAOImpl
        implements CommonPreferencesDAO {

    private final AutoSessionFactory sessionFactory;
    private final Logger log = LogManager.getLogger("Common preferences");

    public CommonPreferencesDAOImpl(AutoSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<CommonPreference> getAll() {
        try (AutoSession session = sessionFactory.openSession()) {
            List<CommonPreference> allCp = session.getAll(CommonPreference.class);
            session.success();
            if (allCp == null || allCp.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Collections.unmodifiableList(allCp);
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to get all common preferences: %s", err.getMessage());
            log.error(errMsg, err);
            return Collections.emptyList();
        }
    }

    public void saveAll(@NotNull List<CommonPreference> preferences) {
        try (AutoSession session = sessionFactory.openSession()) {
            session.saveAll(preferences);
        } catch (Exception err) {
            String errMsg = String.format("Unable to persist common preferences: %s", err.getMessage());
            log.error(errMsg, err);
        }
    }

    private Optional<CommonPreference> upsert(@NotNull final String key,
                                              @NotNull final String stringValue,
                                              final long longValue,
                                              final boolean override) {

        boolean present = false;
        CommonPreference currentValue = null;

        try (AutoSession session = sessionFactory.openSession()) {
            List<CommonPreference> result = session.createQuery("from CommonPreference cp where cp.key = :key",
                    CommonPreference.class)
                    .setParameter("key", key)
                    .list();
            session.success();
            if (result != null && !result.isEmpty()) {
                currentValue = result.get(0);
                present = true;
                if (log.isDebugEnabled()) {
                    log.debug("Value for key \"{}\" present, is \"{}\"", key, currentValue.toString());
                }
            }
        } catch (Exception err) {
            String errMsg = String.format("Unable to fetch common preference value for \"%s\": %s", key, err.getMessage());
            log.error(errMsg, err);
        }

        if (!present || override) {
            CommonPreference newValue = new CommonPreference();
            if (currentValue != null) {
                newValue.setCreateDate(currentValue.getCreateDate());
            } else {
                newValue.setCreateDate(Timestamp.from(Instant.now()));
            }
            newValue.setKey(key);
            newValue.setLongValue(longValue);
            newValue.setStringValue(stringValue);
            newValue.setUpdateDate(Timestamp.from(Instant.now()));

            try (AutoSession session = sessionFactory.openSession()) {
                session.save(newValue);
            } catch (Exception err) {
                String errMsg = String.format("Unable to persist common preference value %s: %s",
                        newValue.toString(), err.getMessage());
                log.error(errMsg, err);
            }
        }

        return Optional.ofNullable(currentValue);
    }

    private String getString(@NotNull String key, @NotNull String defaultValue) {
        return upsert(key, defaultValue, NAN_LONG, false)
                .map(CommonPreference::getStringValue)
                .orElse(defaultValue);
    }

    private String setString(@NotNull String key, @NotNull String value, @NotNull String defaultValue) {
        return upsert(key, value, NAN_LONG, true)
                .map(CommonPreference::getStringValue)
                .orElse(defaultValue);
    }

    private long getLong(@NotNull String key, long defaultValue) {
        return upsert(key, NAN_STRING, defaultValue, false)
                .map(CommonPreference::getLongValue)
                .orElse(defaultValue);
    }

    private long setLong(@NotNull String key, long value, long defaultValue) {
        return upsert(key, NAN_STRING, value, true)
                .map(CommonPreference::getLongValue)
                .orElse(defaultValue);
    }

    @Override
    public String getApiKey() {
        return getString(API_KEY, API_KEY_DEFAULT);
    }

    @Override
    public String setApiKey(@NotNull String newApiKey) {
        return setString(API_KEY, newApiKey, API_KEY_DEFAULT);
    }

    @Override
    public String getBotPrefix() {
        return getString(PREFIX_KEY, PREFIX_DEFAULT);
    }

    @Override
    public String setBotPrefix(@NotNull String newBotPrefix) {
        return setString(PREFIX_KEY, newBotPrefix, PREFIX_DEFAULT);
    }

    @Override
    public String getBotName() {
        return getString(BOT_NAME_KEY, BOT_NAME_DEFAULT);
    }

    @Override
    public String setBotName(@NotNull String newBotName) {
        return setString(BOT_NAME_KEY, newBotName, BOT_NAME_DEFAULT);
    }

    @Override
    public long getOfficialBotServerId() {
        return getLong(OFFICIAL_SERVER_KEY, OFFICIAL_SERVER_DEFAULT);
    }

    @Override
    public long setOfficialBotServerId(long newServerId) {
        return setLong(OFFICIAL_SERVER_KEY, newServerId, OFFICIAL_SERVER_DEFAULT);
    }

    @Override
    public long getBotServiceChannelId() {
        return getLong(SERVICE_CHANNEL_KEY, SERVICE_CHANNEL_DEFAULT);
    }

    @Override
    public long setBotServiceChannelId(long newBotServiceChannelId) {
        return setLong(SERVICE_CHANNEL_KEY, newBotServiceChannelId, SERVICE_CHANNEL_DEFAULT);
    }

    @Override
    public long getHighRollChannelId() {
        return getLong(HIGH_ROLL_CHANNEL_KEY, HIGH_ROLL_CHANNEL_KEY_DEFAULT);
    }

    @Override
    public long setHighRollChannelId(long newHighRollChannelId) {
        return setLong(HIGH_ROLL_CHANNEL_KEY, newHighRollChannelId, HIGH_ROLL_CHANNEL_KEY_DEFAULT);
    }

    @Override
    public long getLowRollChannelId() {
        return getLong(LOW_ROLL_CHANNEL_KEY, LOW_ROLL_CHANNEL_DEFAULT);
    }

    @Override
    public long setLowRollChannelId(long newLowRollChannelId) {
        return setLong(LOW_ROLL_CHANNEL_KEY, newLowRollChannelId, LOW_ROLL_CHANNEL_DEFAULT);
    }
}
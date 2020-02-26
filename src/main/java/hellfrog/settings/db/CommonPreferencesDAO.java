package hellfrog.settings.db;

import com.j256.ormlite.dao.Dao;
import hellfrog.settings.entity.CommonPreference;
import org.jetbrains.annotations.NotNull;

/**
 * Common preferences manager
 */
public interface CommonPreferencesDAO extends Dao<CommonPreference, String> {

    String EMPTY_STRING = "";
    long EMPTY_NUMBER = 0L;
    boolean OVERRIDE = true;
    String PREFIX_KEY = "bot.prefix";
    String PREFIX_DEFAULT = ">>";
    String API_KEY = "api.key";
    String API_KEY_DEFAULT = "";
    String BOT_NAME_KEY = "bot.name";
    String BOT_NAME_DEFAULT = "HellFrog";
    String OFFICIAL_SERVER_KEY = "bot.server";
    long OFFICIAL_SERVER_DEFAULT = 612645599132778517L;
    String SERVICE_CHANNEL_KEY = "bot.service.channel";
    long SERVICE_CHANNEL_DEFAULT = 612659329392443422L;
    String HIGH_ROLL_CHANNEL_KEY = "bot.high.channel";
    long HIGH_ROLL_CHANNEL_DEFAULT = 612654844679028736L;
    String LOW_ROLL_CHANNEL_KEY = "bot.low.channel";
    long LOW_ROLL_CHANNEL_DEFAULT = 612654929219158021L;

    String getApiKey();

    String setApiKey(@NotNull String newApiKey);

    String getBotPrefix();

    String setBotPrefix(@NotNull String newBotPrefix);

    String getBotName();

    String setBotName(@NotNull String newBotName);

    long getOfficialBotServerId();

    long setOfficialBotServerId(long newServerId);

    long getBotServiceChannelId();

    long setBotServiceChannelId(long newBotServiceChannelId);

    long getHighRollChannelId();

    long setHighRollChannelId(long newHighRollChannelId);

    long getLowRollChannelId();

    long setLowRollChannelId(long newLowRollChannelId);
}

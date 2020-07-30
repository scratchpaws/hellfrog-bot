package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

public interface CommonPreferencesDAO {

    long NAN_LONG = 0L;
    String NAN_STRING = "0";

    long OFFICIAL_SERVER_DEFAULT = 612645599132778517L;
    long SERVICE_CHANNEL_DEFAULT = 612659329392443422L;
    long HIGH_ROLL_CHANNEL_KEY_DEFAULT = 612654844679028736L;
    long LOW_ROLL_CHANNEL_DEFAULT = 612654929219158021L;

    String PREFIX_DEFAULT = ">>";
    String API_KEY_DEFAULT = "";
    String BOT_NAME_DEFAULT = "HellFrog";
    String PREFIX_KEY = "bot.prefix";
    String API_KEY = "api.key";
    String BOT_NAME_KEY = "bot.name";
    String OFFICIAL_SERVER_KEY = "bot.server";
    String OFFICIAL_SERVER_DEFAULT_STR = String.valueOf(OFFICIAL_SERVER_DEFAULT);
    String SERVICE_CHANNEL_KEY = "bot.service.channel";
    String SERVICE_CHANNEL_DEFAULT_STR = String.valueOf(SERVICE_CHANNEL_DEFAULT);
    String HIGH_ROLL_CHANNEL_KEY = "bot.high.channel";
    String HIGH_ROLL_CHANNEL_KEY_DEFAULT_STR = String.valueOf(HIGH_ROLL_CHANNEL_KEY_DEFAULT);
    String LOW_ROLL_CHANNEL_KEY = "bot.low.channel";
    String LOW_ROLL_CHANNEL_DEFAULT_STR = String.valueOf(LOW_ROLL_CHANNEL_DEFAULT);

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

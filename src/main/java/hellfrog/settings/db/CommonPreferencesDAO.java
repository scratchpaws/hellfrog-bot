package hellfrog.settings.db;

import org.jetbrains.annotations.NotNull;

public interface CommonPreferencesDAO {

    String PREFIX_DEFAULT = ">>";
    String API_KEY_DEFAULT = "";
    String BOT_NAME_DEFAULT = "HellFrog";
    long OFFICIAL_SERVER_DEFAULT = 612645599132778517L;
    long SERVICE_CHANNEL_DEFAULT = 612659329392443422L;
    long HIGH_ROLL_CHANNEL_KEY_DEFAULT = 612654844679028736L;
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
